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
     * 주의: Redis 재고 롤백은 PaymentService에서 이미 수행됨.
     * 여기서는 DB 상태 동기화만 수행함.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processPaymentFailDB(Order order) {

        // DB 재고 동기화
        // Redis가 Master이므로 굳이 여기서 DB Lock을 걸고 업데이트할 필요는 없음.
        // 다만, 관리자 페이지 등에서 보기 위해 '락 없이' 단순 증가만 수행하거나, 아예 생략하고 스케줄러에 맡겨도 됨.
        // 여기서는 안전하게 락 제거하고 로직만 유지함.
        for (OrderItem item : order.getOrderItems()) {
            try {
                skuJPARepository.findById(item.getSku().getId()).ifPresent(sku -> {
                    sku.increaseStock(item.getQuantity());
                });
            } catch (Exception e) {
                log.error("[Payment Fail] DB 재고 복구 실패 (Redis는 롤백됨): {}", e.getMessage());
            }
        }

        // 2. Order 상태 변경 (FAILED)
        order.setOrderStatus(OrderStatus.FAILED);

        // 3. 변경 사항 저장
        orderRepository.save(order);
    }

    /**
     * [핵심] 결제 성공 시 DB 저장 대신 Redis에 Push (Write-Behind)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processPaymentSuccessDB(Order order, Map<String, Object> tossResponse, Long userId) {

        // 1. 데이터 추출
        String tossPaymentKey = (String) tossResponse.get("paymentKey");
        String method = (String) tossResponse.get("method");
        String status = (String) tossResponse.get("status");
        String paidAtString = (String) tossResponse.get("paidAt");
        LocalDateTime paidAt = (paidAtString != null) ? LocalDateTime.parse(paidAtString) : null; // ISO 파싱 주의 (필요시 포맷터 사용)

        // JSON 변환
        String pgRawData = "";
        try {
            pgRawData = objectMapper.writeValueAsString(tossResponse);
        } catch (Exception e) {
            log.warn("결제 정보 JSON 변환 실패: {}", e.getMessage());
        }

        // 2. [Redis 저장] PendingPayment DTO 생성 및 Push
        PendingPayment pendingPayment = new PendingPayment(
                order.getOrderNumber(),
                tossPaymentKey,
                order.getTotalAmount(),
                method,
                status,
                paidAt,
                pgRawData
        );

        redisStockService.pushPendingPayment(pendingPayment);

        // 3. 장바구니 삭제 (최적화)
        try {
            List<Long> purchasedSkuIds = order.getOrderItems().stream()
                    .map(orderItem -> orderItem.getSku().getId())
                    .collect(Collectors.toList());

            if (!purchasedSkuIds.isEmpty()) {
                // [수정] 반복문 제거 -> 한 방 쿼리로 삭제
                cartRepository.deleteByUserIdAndSkuIdIn(userId, purchasedSkuIds);
                log.info("[Payment Success] 장바구니 삭제 완료 (UserId: {})", userId);
            }
        } catch (Exception e) {
            log.error("[Payment Success] 장바구니 삭제 오류: {}", e.getMessage());
        }
    }

    /**
     * 결제 취소 시 DB 처리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processPaymentCancelDB(Order order, Payment payment, Map<String, Object> tossResponse) {

        // 1. DB 재고 복구 (락 제거)
        for (OrderItem item : order.getOrderItems()) {
            try {
                // [수정] 락 제거
                skuJPARepository.findById(item.getSku().getId()).ifPresent(sku -> {
                    sku.increaseStock(item.getQuantity());
                });
            } catch (Exception e) {
                log.error("[Payment Cancel] DB 재고 복구 실패: {}", e.getMessage());
            }
        }

        // 2. DB 상태 업데이트
        order.setOrderStatus(OrderStatus.FAILED); // 또는 CANCELLED
        order.setDeliveryStatus(DeliveryStatus.CANCELLED);
        payment.setPaymentStatus("CANCELED");

        orderRepository.save(order);
        paymentRepository.save(payment);

        log.info("주문 취소 DB 처리 완료: orderId={}", order.getId());
    }
}