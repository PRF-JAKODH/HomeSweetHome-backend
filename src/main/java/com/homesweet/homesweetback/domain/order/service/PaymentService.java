package com.homesweet.homesweetback.domain.order.service;

// --- DTO Imports ---
import com.homesweet.homesweetback.domain.order.dto.request.OrderCancelRequest;
import com.homesweet.homesweetback.domain.order.dto.request.PaymentConfirmRequest;
import com.homesweet.homesweetback.domain.order.dto.response.PaymentConfirmResponse;

// --- Entity Imports ---
import com.homesweet.homesweetback.domain.order.entity.*;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;

// --- Repository Imports ---
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.order.repository.PaymentRepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.SkuJPARepository;

// --- Exception Imports ---
import com.homesweet.homesweetback.common.exception.OrderNotFoundException;
import com.homesweet.homesweetback.common.exception.PaymentMismatchException;
import jakarta.persistence.EntityNotFoundException;

// --- Spring & Java Imports ---
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    // --- (수정) 결제 처리에 필요한 Repository 및 Bean 주입 ---
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SkuJPARepository skuJPARepository; // ★ 재고 차감용
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${payments.toss.secretKey}")
    private String tossSecretKey;

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String TOSS_CANCEL_URL_PREFIX = "https://api.tosspayments.com/v1/payments/";

    /**
     * API 2: 결제 검증 및 완료
     * (재고 차감 로직 포함)
     */
    @Transactional
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest dto, Long userId) {
        // dto의 orderId와 order의 id는 같지 않다.
        // dto.orderId == order.orderNumber
        String[] s = dto.orderId().split("-");

        // 1. [검증 1] Order ID (PK)로 DB에서 Order 조회
        Order order = orderRepository.findById(Long.parseLong(s[2]))
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다: " + dto.orderId()));
        log.debug(order.toString());
        log.debug(order.getOrderStatus().toString());
        log.debug(order.getTotalAmount().toString());
        log.debug(dto.amount().toString());
        // 2. [검증 2] (보안) 요청한 유저(userId)가 주문한 유저가 맞는지 확인
        if (!order.getUser().getId().equals(userId)) {
            throw new PaymentMismatchException("주문자 정보가 일치하지 않습니다.");
        }

        // 3. [검증 3] (핵심) DB의 주문 금액과 토스가 전달한 결제 금액이 일치하는지 확인
        if (!order.getTotalAmount().equals(dto.amount())) {
            // (TODO: 금액 위변조 의심. 결제 취소 API를 호출해야 함)
            throw new PaymentMismatchException("결제 금액이 일치하지 않습니다.");
        }

        // 4. [검증 4] (멱등성)
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new PaymentMismatchException("이미 처리된 주문입니다.");
        }

        // 5. [★핵심★] 토스페이먼츠 결제 승인 API 호출
        HttpHeaders headers = new HttpHeaders();
        String encodedKey = Base64.getEncoder().encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        headers.setBasicAuth(encodedKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PaymentConfirmRequest> requestEntity = new HttpEntity<>(dto, headers);

        Map<String, Object> tossResponse;
        try {
            tossResponse = restTemplate.postForObject(TOSS_CONFIRM_URL, requestEntity, Map.class);
        } catch (Exception e) {
            order.setOrderStatus(OrderStatus.FAILED); // (가정한 메서드)
            throw new RuntimeException("토스페이먼츠 승인 API 호출에 실패했습니다. " + e.getMessage());
        }

        // 6. [DB 저장 1] 토스 응답 기반으로 Payment 엔티티 생성
        String tossPaymentKey = (String) tossResponse.get("paymentKey");
        String method = (String) tossResponse.get("method");
        String status = (String) tossResponse.get("status");

        if (!"DONE".equals(status)) {
            order.setOrderStatus(OrderStatus.FAILED);
            throw new RuntimeException("결제가 완료되지 않았습니다. 상태: " + status);
        }

        String paidAtString = (String) tossResponse.get("paidAt");
        LocalDateTime paidAt = (paidAtString != null) ? LocalDateTime.parse(paidAtString) : null;

        Payment payment;
        try {
            payment = Payment.builder()
                    .order(order)
                    .pgTransactionId(tossPaymentKey)
                    .amount(order.getTotalAmount())
                    .method(method)
                    .paymentStatus(status) // "DONE"
                    .paidAt(paidAt)
                    .pgRawData(objectMapper.writeValueAsString(tossResponse))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Payment 엔티티 생성 중 오류 발생");
        }

        paymentRepository.save(payment);

        // 7. [DB 저장 2] Order 상태 변경 (PENDING -> COMPLETED)
        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setDeliveryStatus(DeliveryStatus.DELIVERED); // 결제 즉시 배송 완료 처리
        // 8. [DB 저장 3] (★중요★) 재고 차감 (비관적 락 사용)
        for (OrderItem item : order.getOrderItems()) {
            SkuEntity sku = skuJPARepository.findByIdWithPessimisticLock(item.getSku().getId())
                    .orElseThrow(() -> new EntityNotFoundException("SKU를 찾을 수 없습니다: " + item.getSku().getId()));

            // (가정한 메서드) SkuEntity의 재고 차감 로직 호출
            sku.decreaseStock(item.getQuantity());
        }

        // 9. 최종 응답 반환
        return new PaymentConfirmResponse(
                order.getId(),
                order.getOrderStatus().name()
        );
    }

    /**
     * API 3: 주문 취소 (환불)
     * (토스 API 호출, 재고 복구)
     */
    @Transactional
    public void cancelOrder(Long orderId, Long userId, OrderCancelRequest dto) {

        // 1. 주문 조회 (모든 연관 엔티티 포함)
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다: " + orderId));

        // 2. (보안) 주문자 본인 확인
        if (!order.getUser().getId().equals(userId)) {
            throw new PaymentMismatchException("주문 정보에 접근할 권한이 없습니다.");
        }

        // 3. (상태 검증) 이미 취소된 주문인지 확인
        if (order.getDeliveryStatus() == DeliveryStatus.CANCELLED) {
            throw new RuntimeException("이미 취소된 주문입니다."); // (커스텀 예외 권장)
        }

        // 4. 결제 정보 조회
        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new EntityNotFoundException("결제 정보를 찾을 수 없습니다. (비정상 상태)"));

        // 5. (★핵심★) 토스페이먼츠 결제 취소 API 호출
        String tossPaymentKey = payment.getPgTransactionId();
        URI cancelUrl = URI.create(TOSS_CANCEL_URL_PREFIX + tossPaymentKey + "/cancel");

        HttpHeaders headers = new HttpHeaders();
        String encodedKey = Base64.getEncoder().encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        headers.setBasicAuth(encodedKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 토스 API로 보낼 요청 본문 (취소 사유)
        Map<String, String> bodyMap = Collections.singletonMap("cancelReason", dto.cancelReason());
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(bodyMap, headers);

        Map<String, Object> tossResponse;
        try {
            log.info("토스 결제 취소 API 호출: paymentKey={}, reason={}", tossPaymentKey, dto.cancelReason());
            tossResponse = restTemplate.postForObject(cancelUrl, requestEntity, Map.class);
        } catch (Exception e) {
            log.error("토스페이먼츠 취소 API 호출 실패: {}", e.getMessage());
            // (TODO: 토스 API 통신 실패 시 어떻게 처리할지 정책 필요)
            throw new RuntimeException("결제 취소 API 호출에 실패했습니다. " + e.getMessage());
        }

        // 6. 응답 상태 확인 (토스 응답에 'status' 필드가 있는지 확인 필요)
        String status = (String) tossResponse.get("status");
        if (!"CANCELED".equals(status)) { // (토스 응답 스펙 확인 필요)
            log.warn("토스 결제 취소 응답 상태가 CANCELED가 아닙니다: {}", status);
            // (이 경우에도 재고 복구 등을 해야 할지 정책 필요)
        }

        // 7. (★중요★) 재고 복구
        for (OrderItem item : order.getOrderItems()) {
            // (동시성 제어를 위해 락 사용)
            SkuEntity sku = skuJPARepository.findByIdWithPessimisticLock(item.getSku().getId())
                    .orElseThrow(() -> new EntityNotFoundException("SKU를 찾을 수 없습니다: " + item.getSku().getId()));

            sku.increaseStock(item.getQuantity()); // (가정한 메서드)
        }

        // 8. DB 상태 업데이트
        order.setOrderStatus(OrderStatus.FAILED); // (정책: 취소 시 '결제 실패'로 처리)
        order.setDeliveryStatus(DeliveryStatus.CANCELLED);
        payment.setPaymentStatus("CANCELED"); // Payment 상태도 변경

        log.info("주문 취소 완료 (환불 및 재고 복구): orderId={}", orderId);
    }
}