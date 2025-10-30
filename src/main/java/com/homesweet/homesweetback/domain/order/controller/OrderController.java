package com.homesweet.homesweetback.domain.order.controller;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.order.dto.request.CreateOrderRequest;
import com.homesweet.homesweetback.domain.order.dto.request.PaymentConfirmRequest;
import com.homesweet.homesweetback.domain.order.dto.response.OrderReadyResponse;
import com.homesweet.homesweetback.domain.order.dto.response.PaymentConfirmResponse;
import com.homesweet.homesweetback.domain.order.service.OrderService;
import com.homesweet.homesweetback.domain.order.service.PaymentService; // ★ PaymentService import
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    /**
     * API 1: 주문 생성 (결제 준비)
     * (OrderService 호출)
     */
    @PostMapping
    public ResponseEntity<OrderReadyResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest dto,
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ) {
        Long userId = principal.getUserId();

        OrderReadyResponse response = orderService.createOrder(dto, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * API 2: 결제 검증 및 완료
     * (PaymentService 호출)
     */
    @PostMapping("/payments/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirmPayment(
            @Valid @RequestBody PaymentConfirmRequest dto,
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ) {
        Long userId = principal.getUserId();

        // --- (수정) PaymentService 호출 ---
        PaymentConfirmResponse response = paymentService.confirmPayment(dto, userId);
        return ResponseEntity.ok(response);
    }
}