package com.homesweet.homesweetback.domain.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.homesweetback.domain.order.entity.*;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.order.repository.PaymentRepository;
import com.homesweet.homesweetback.domain.product.cart.repository.CartRepository;
import com.homesweet.homesweetback.domain.order.dto.internal.PendingPayment;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.SkuJPARepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.SkuEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 결제 관련 DB 트랜잭션 처리를 전담하는 프로세서
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessor {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SkuJPARepository skuJPARepository;
    private final CartRepository cartRepository;
    private final ObjectMapper objectMapper;
    private final RedisStockService redisStockService;

    /**
     * 결제 실패 시 DB 처리 (API 호출 실패 시)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processPaymentFailDB(Order order) {
        // 1. 재고 복구
        for (OrderItem item : order.getOrderItems()) {
            try {
                SkuEntity sku = skuJPARepository.findByIdWithPessimisticLock(item.getSku().getId())
                        .orElseThrow(() -> new EntityNotFoundException("SKU를 찾을 수 없습니다: " + item.getSku().getId()));
                sku.increaseStock(item.getQuantity());
            } catch (Exception e) {
                log.error("[Payment Fail - Stock Restore Failed] 재고 복구 오류. (OrderId: {}): {}", order.getId(), e.getMessage());
            }
        }

        // 2. Order 상태 변경 (FAILED)
        order.setOrderStatus(OrderStatus.FAILED);

        // 3. 변경 사항 저장 (실패 처리는 DB에 바로 반영해도 무방함)
        orderRepository.save(order);
    }

    /**
     * [핵심 수정] 결제 성공 시 DB 저장 대신 Redis에 Push (Write-Behind)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processPaymentSuccessDB(Order order, Map<String, Object> tossResponse, Long userId) {

        // 1. 데이터 추출
        String tossPaymentKey = (String) tossResponse.get("paymentKey");
        String method = (String) tossResponse.get("method");
        String status = (String) tossResponse.get("status");
        String paidAtString = (String) tossResponse.get("paidAt");
        LocalDateTime paidAt = (paidAtString != null) ? LocalDateTime.parse(paidAtString) : null;

        // JSON 변환 (예외 처리 포함)
        String pgRawData = "";
        try {
            pgRawData = objectMapper.writeValueAsString(tossResponse);
        } catch (Exception e) {
            log.warn("결제 정보 JSON 변환 실패: {}", e.getMessage());
        }

        // 2. [Redis 저장] PendingPayment DTO 생성 및 Push
        PendingPayment pendingPayment = new PendingPayment(
                order.getOrderNumber(), // Order ID 대신 Number 사용
                tossPaymentKey,
                order.getTotalAmount(),
                method,
                status,
                paidAt,
                pgRawData
        );

        // DB 저장(save) 코드는 삭제하고 Redis에 넣습니다.
        redisStockService.pushPendingPayment(pendingPayment);


        // 3. 장바구니 삭제 (이건 여기서 수행)
        try {
            List<Long> purchasedSkuIds = order.getOrderItems().stream()
                    .map(orderItem -> orderItem.getSku().getId())
                    .collect(Collectors.toList());

            if (!purchasedSkuIds.isEmpty()) {
                for (Long skuId : purchasedSkuIds) {
                    // (CartRepository에 deleteByUserIdAndSkuId 메서드가 필요함)
                    // 만약 없다면 JPARepository에 "void deleteByUserIdAndSkuId(Long userId, Long skuId);" 추가 필요
                    cartRepository.deleteByUserIdAndSkuIdIn(userId, List.of(skuId));
                }
                log.info("[Payment Success] 장바구니 삭제 완료 (UserId: {})", userId);
            }
        } catch (Exception e) {
            log.error("[Payment Success - Cart Clear Failed] 장바구니 삭제 오류. (UserId: {}): {}", userId, e.getMessage());
        }
    }

    /**
     * 결제 취소 시 DB 처리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processPaymentCancelDB(Order order, Payment payment, Map<String, Object> tossResponse) {

        // 재고 복구
        for (OrderItem item : order.getOrderItems()) {
            try {
                SkuEntity sku = skuJPARepository.findByIdWithPessimisticLock(item.getSku().getId())
                        .orElseThrow(() -> new EntityNotFoundException("SKU를 찾을 수 없습니다: " + item.getSku().getId()));

                sku.increaseStock(item.getQuantity());
            } catch (Exception e) {
                log.error("[Payment Cancel - Stock Restore Failed] 재고 복구 오류. (OrderId: {}): {}", order.getId(), e.getMessage());
            }
        }

        // DB 상태 업데이트
        order.setOrderStatus(OrderStatus.FAILED);
        order.setDeliveryStatus(DeliveryStatus.CANCELLED);
        payment.setPaymentStatus("CANCELED");

        orderRepository.save(order);

        // 👇 [추가] Payment 변경 사항을 DB에 강제로 저장 (UPDATE)
        paymentRepository.save(payment);

        log.info("주문 취소 DB 처리 완료: orderId={}", order.getId());
    }
}