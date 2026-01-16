package com.homesweet.homesweetback.domain.order.service;

import com.homesweet.homesweetback.common.exception.OrderNotFoundException;
import com.homesweet.homesweetback.common.exception.PaymentMismatchException;
import com.homesweet.homesweetback.domain.order.dto.PaymentResponse;
import com.homesweet.homesweetback.domain.order.dto.TossPaymentCancelRequest;
import com.homesweet.homesweetback.domain.order.dto.TossPaymentConfirmRequest;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.Payment;
import com.homesweet.homesweetback.domain.order.entity.PaymentStatus;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.order.repository.PaymentRepository;
import com.homesweet.homesweetback.domain.product.cart.repository.jpa.CartJPARepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 결제 서비스 구현체
 * 토스페이먼츠 API 연동 및 결제 비즈니스 로직 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final TossPaymentsService tossPaymentsService;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CartJPARepository cartJPARepository;

    /**
     * 결제 승인 처리
     * 토스페이먼츠 결제창 완료 후 successUrl에서 호출
     *
     * 토스페이먼츠 문서에 따른 처리:
     * 1. orderId로 주문 조회
     * 2. amount 검증 (클라이언트 금액 조작 방지)
     * 3. 결제 승인 API 호출
     * 4. Payment 엔티티 저장
     * 5. Order 상태 변경
     * 6. 장바구니에서 구매 완료된 상품 삭제
     */
    @Override
    @Transactional
    public PaymentResponse confirmPayment(Long userId, TossPaymentConfirmRequest request) {
        log.info("결제 승인 처리 시작: userId={}, orderId={}, amount={}",
                userId, request.getOrderId(), request.getAmount());

        // 1. 주문 조회 (orderId = orderNumber)
        Order order = orderRepository.findByOrderNumberWithItems(request.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다. orderId=" + request.getOrderId()));

        // 권한 확인
        if (!order.isOwner(userId)) {
            throw new IllegalArgumentException("본인의 주문만 결제할 수 있습니다.");
        }

        // 2. 금액 검증 (토스페이먼츠 문서: 클라이언트 금액 조작 방지)
        if (!order.getTotalAmount().equals(request.getAmount())) {
            log.error("결제 금액 불일치: 주문금액={}, 요청금액={}",
                    order.getTotalAmount(), request.getAmount());
            throw new PaymentMismatchException("결제 금액이 주문 금액과 일치하지 않습니다.");
        }

        // 3. 토스페이먼츠 결제 승인 API 호출
        Map<String, Object> tossResponse = tossPaymentsService.confirmPayment(request);

        // 4. Payment 엔티티 생성 및 저장
        Payment payment = Payment.builder()
                .order(order)
                .paymentKey(request.getPaymentKey())
                .tossOrderId(request.getOrderId())
                .status(PaymentStatus.READY)
                .amount(request.getAmount())
                .requestedAt(parseDateTime(tossResponse.get("requestedAt")))
                .build();

        // 토스 응답에서 정보 추출하여 결제 완료 처리
        String method = extractString(tossResponse, "method");
        LocalDateTime approvedAt = parseDateTime(tossResponse.get("approvedAt"));
        String receiptUrl = extractReceiptUrl(tossResponse);

        payment.complete(method, approvedAt, receiptUrl);
        paymentRepository.save(payment);

        // 5. 주문 상태 변경
        order.pay();
        orderRepository.save(order);

        // 6. 장바구니에서 구매 완료된 SKU 삭제
        List<Long> purchasedSkuIds = order.getOrderItems().stream()
                .map(item -> item.getSku().getId())
                .toList();
        cartJPARepository.deleteCartItemNative(userId, purchasedSkuIds);

        log.info("결제 승인 완료: paymentKey={}, orderId={}", request.getPaymentKey(), order.getId());
        return PaymentResponse.from(payment);
    }

    /**
     * 결제 취소
     */
    @Override
    @Transactional
    public PaymentResponse cancelPayment(Long userId, String paymentKey, TossPaymentCancelRequest request) {
        log.info("결제 취소 처리 시작: userId={}, paymentKey={}", userId, paymentKey);

        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));

        // 권한 확인
        if (!payment.getOrder().isOwner(userId)) {
            throw new IllegalArgumentException("본인의 결제만 취소할 수 있습니다.");
        }

        // 토스페이먼츠 취소 API 호출
        tossPaymentsService.cancelPayment(paymentKey, request);

        // 결제 상태 변경
        if (request.getCancelAmount() != null && request.getCancelAmount() < payment.getAmount()) {
            payment.partialCancel();
        } else {
            payment.cancel();
            payment.getOrder().cancel();
        }

        paymentRepository.save(payment);
        log.info("결제 취소 완료: paymentKey={}", paymentKey);

        return PaymentResponse.from(payment);
    }

    /**
     * 결제 조회
     */
    @Override
    public PaymentResponse getPayment(Long userId, String paymentKey) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));

        if (!payment.getOrder().isOwner(userId)) {
            throw new IllegalArgumentException("본인의 결제만 조회할 수 있습니다.");
        }

        return PaymentResponse.from(payment);
    }

    // ===== Helper Methods =====

    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private String extractReceiptUrl(Map<String, Object> tossResponse) {
        Object receipt = tossResponse.get("receipt");
        if (receipt instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> receiptMap = (Map<String, Object>) receipt;
            Object url = receiptMap.get("url");
            return url != null ? url.toString() : null;
        }
        return null;
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.toString()).toLocalDateTime();
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}", value);
            return null;
        }
    }
}
