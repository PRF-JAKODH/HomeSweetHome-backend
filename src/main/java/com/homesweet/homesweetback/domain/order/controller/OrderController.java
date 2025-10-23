package com.homesweet.homesweetback.domain.order.controller;

import com.homesweet.homesweetback.domain.order.dto.request.CreateOrderRequest;
import com.homesweet.homesweetback.domain.order.dto.request.PaymentConfirmRequest;
import com.homesweet.homesweetback.domain.order.dto.response.OrderReadyResponse;
import com.homesweet.homesweetback.domain.order.dto.response.PaymentConfirmResponse;
import com.homesweet.homesweetback.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // 이 클래스가 REST API 컨트롤러임을 선언
@RequiredArgsConstructor // final OrderService 주입
@RequestMapping("/api/v1/orders") // 이 컨트롤러의 모든 API는 "/api/v1/orders"로 시작
public class OrderController {

    private final OrderService orderService;


     // API 1: 주문 생성 (결제 준비)
     // POST /api/v1/orders
    @PostMapping
    public ResponseEntity<OrderReadyResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest dto
    ) {
        Long tempUserId = 1L;

        // 1. Service에 로직 위임
        OrderReadyResponse response = orderService.createOrder(dto, tempUserId);

        // 2. 성공 응답 (200 OK) 반환
        return ResponseEntity.ok(response);
    }

    /**
     * API 2: 결제 검증 및 완료
     * POST /api/v1/orders/payments/confirm
     * (참고: API 명세에는 /api/v1/payments/confirm 이었으나,
     * /api/v1/orders 하위로 경로를 변경하는 것이 더 RESTful 합니다.)
     */
    @PostMapping("/payments/confirm") // "/api/v1/orders" + "/payments/confirm"
    public ResponseEntity<PaymentConfirmResponse> confirmPayment(
            @Valid @RequestBody PaymentConfirmRequest dto
            // TODO: (인증) @AuthenticationPrincipal UserDetails user
    ) {
        // (임시) TODO: Spring Security 설정 후, 실제 인증된 userId를 가져와야 함
        Long tempUserId = 1L;

        // 1. Service에 로직 위임
        PaymentConfirmResponse response = orderService.confirmPayment(dto, tempUserId);

        // 2. 성공 응답 (200 OK) 반환
        return ResponseEntity.ok(response);
    }
}