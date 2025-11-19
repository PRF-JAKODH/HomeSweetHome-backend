package com.homesweet.homesweetback.domain.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.homesweetback.common.exception.OrderNotFoundException;
import com.homesweet.homesweetback.domain.order.entity.*;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.order.repository.PaymentRepository;
import com.homesweet.homesweetback.domain.product.cart.repository.CartRepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.SkuJPARepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 결제 관련 DB 트랜잭션 처리를 전담하는 프로세서
 * (PaymentService의 셀프 호출 문제를 해결하기 위해 분리됨)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessor {

    private final OrderRepository orderRepository; // (상태 변경용)
    private final PaymentRepository paymentRepository;
    private final SkuJPARepository skuJPARepository;
    private final CartRepository cartRepository;
    private final ObjectMapper objectMapper;

    /**
     * 결제 실패 시 DB 처리 (API 호출 실패 시)
     */
    @Transactional
    public void processPaymentFailDB(Order order) {
        // 1. 재고 복구 (이미 차감된 재고를 다시 늘림)
        for (OrderItem item : order.getOrderItems()) {
            try {
                // 동시성 제어를 위해 락을 걸고 조회
                SkuEntity sku = skuJPARepository.findByIdWithPessimisticLock(item.getSku().getId())
                        .orElseThrow(() -> new EntityNotFoundException("SKU를 찾을 수 없습니다: " + item.getSku().getId()));

                // 재고 증가
                sku.increaseStock(item.getQuantity());

            } catch (Exception e) {
                // 재고 복구 실패 시 로그 남김 (트랜잭션 롤백 방지)
                log.error("[Payment Fail - Stock Restore Failed] 결제 실패 처리 중 재고 복구 오류. (OrderId: {}): {}",
                        order.getId(), e.getMessage());
            }
        }

        // 2. Order 상태 변경 (FAILED)
        order.setOrderStatus(OrderStatus.FAILED);

        // 3. 변경 사항 저장
        orderRepository.save(order);
    }

    /**
     * 결제 성공 시 DB 처리
     */
    @Transactional
    public void processPaymentSuccessDB(Order order, Map<String, Object> tossResponse, Long userId) {
        // [DB 저장 1] Payment 엔티티 생성
        String tossPaymentKey = (String) tossResponse.get("paymentKey");
        String method = (String) tossResponse.get("method");
        String status = (String) tossResponse.get("status");
        String paidAtString = (String) tossResponse.get("paidAt");
        LocalDateTime paidAt = (paidAtString != null) ? LocalDateTime.parse(paidAtString) : null;

        Payment payment;
        try {
            payment = Payment.builder()
                    .order(order)
                    .pgTransactionId(tossPaymentKey)
                    .amount(order.getTotalAmount())
                    .method(method)
                    .paymentStatus(status)
                    .paidAt(paidAt)
                    .pgRawData(objectMapper.writeValueAsString(tossResponse))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Payment 엔티티 생성 중 오류 발생");
        }
        paymentRepository.save(payment);

        // [DB 저장 2] Order 상태 변경
        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setDeliveryStatus(DeliveryStatus.DELIVERED);

        // 트랜잭션이 분리되었으므로, 명시적으로 save(update)를 호출해 줍니다.
        orderRepository.save(order);

        // [DB 저장 4] 장바구니 삭제
        try {
            List<Long> purchasedSkuIds = order.getOrderItems().stream()
                    .map(orderItem -> orderItem.getSku().getId())
                    .collect(Collectors.toList());

            if (!purchasedSkuIds.isEmpty()) {
                cartRepository.deleteByUserIdAndSkuIdIn(userId, purchasedSkuIds);
                log.info("[Payment Success] 장바구니 삭제 완료 (UserId: {})", userId);
            }
        } catch (Exception e) {
            log.error("[Payment Success - Cart Clear Failed] 장바구니 삭제 중 오류 발생. (UserId: {}): {}", userId, e.getMessage());
        }
    }

    /**
     * 결제 취소 시 DB 처리
     */
    @Transactional
    public void processPaymentCancelDB(Order order, Payment payment, Map<String, Object> tossResponse) {

        // 재고 복구 (try-catch로 감싸서 실패해도 상태 변경은 되도록 함)
        for (OrderItem item : order.getOrderItems()) {
            try {
                SkuEntity sku = skuJPARepository.findByIdWithPessimisticLock(item.getSku().getId())
                        .orElseThrow(() -> new EntityNotFoundException("SKU를 찾을 수 없습니다: " + item.getSku().getId()));

                sku.increaseStock(item.getQuantity());
            } catch (Exception e) {
                log.error("[Payment Cancel - Stock Restore Failed] 재고 복구 중 오류 발생. (OrderId: {}): {}",
                        order.getId(), e.getMessage());
            }
        }

        // DB 상태 업데이트
        order.setOrderStatus(OrderStatus.FAILED);
        order.setDeliveryStatus(DeliveryStatus.CANCELLED);
        payment.setPaymentStatus("CANCELED");
        log.info("주문 취소 DB 처리 완료 (환불 및 재고 복구): orderId={}", order.getId());

        orderRepository.save(order);
    }
}