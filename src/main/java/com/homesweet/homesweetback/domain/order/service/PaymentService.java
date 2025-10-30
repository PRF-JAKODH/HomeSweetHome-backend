package com.homesweet.homesweetback.domain.order.service;

// --- DTO Imports ---
import com.homesweet.homesweetback.domain.order.dto.request.PaymentConfirmRequest;
import com.homesweet.homesweetback.domain.order.dto.response.PaymentConfirmResponse;

// --- Entity Imports ---
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderItem;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.order.entity.Payment;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
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
        // (deliveryStatus는 BEFORE_SHIPMENT 상태 유지)

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
}