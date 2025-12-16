package com.homesweet.homesweetback.domain.order.controller;

import com.homesweet.homesweetback.domain.order.dto.TossPaymentCancelRequest;
import com.homesweet.homesweetback.domain.order.dto.TossPaymentConfirmRequest;
import com.homesweet.homesweetback.domain.order.service.TossPaymentsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 토스페이먼츠 결제 API 컨트롤러
 * 
 * 결제 흐름:
 * 1. 클라이언트에서 토스 결제 위젯으로 결제 요청
 * 2. 결제 완료 후 successUrl로 리다이렉트되면서 paymentKey, orderId, amount 전달
 * 3. POST /api/v1/payments/confirm 호출하여 결제 승인
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final TossPaymentsService tossPaymentsService;

    /**
     * 결제 승인 API
     * 프론트엔드에서 토스 결제창 완료 후 받은 paymentKey, orderId, amount로 최종 결제 승인
     * 
     * @param request 결제 승인 요청 (paymentKey, orderId, amount)
     * @return 토스페이먼츠 결제 승인 응답
     */
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> confirmPayment(
            @RequestBody TossPaymentConfirmRequest request) {
        log.info("결제 승인 API 호출: orderId={}, amount={}", request.getOrderId(), request.getAmount());

        Map<String, Object> response = tossPaymentsService.confirmPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 취소 API
     * 
     * @param paymentKey 취소할 결제의 paymentKey
     * @param request    취소 사유 및 부분 취소 금액 (선택)
     * @return 토스페이먼츠 결제 취소 응답
     */
    @PostMapping("/{paymentKey}/cancel")
    public ResponseEntity<Map<String, Object>> cancelPayment(
            @PathVariable String paymentKey,
            @RequestBody TossPaymentCancelRequest request) {
        log.info("결제 취소 API 호출: paymentKey={}, reason={}", paymentKey, request.getCancelReason());

        Map<String, Object> response = tossPaymentsService.cancelPayment(paymentKey, request);
        return ResponseEntity.ok(response);
    }

    /**
     * paymentKey로 결제 조회 API
     * 
     * @param paymentKey 조회할 결제의 paymentKey
     * @return 토스페이먼츠 결제 정보
     */
    @GetMapping("/{paymentKey}")
    public ResponseEntity<Map<String, Object>> getPaymentByPaymentKey(
            @PathVariable String paymentKey) {
        log.info("결제 조회 API 호출: paymentKey={}", paymentKey);

        Map<String, Object> response = tossPaymentsService.getPaymentByPaymentKey(paymentKey);
        return ResponseEntity.ok(response);
    }

    /**
     * orderId로 결제 조회 API
     * 
     * @param orderId 조회할 주문의 orderId
     * @return 토스페이먼츠 결제 정보
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> getPaymentByOrderId(
            @PathVariable String orderId) {
        log.info("주문ID로 결제 조회 API 호출: orderId={}", orderId);

        Map<String, Object> response = tossPaymentsService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }
}
