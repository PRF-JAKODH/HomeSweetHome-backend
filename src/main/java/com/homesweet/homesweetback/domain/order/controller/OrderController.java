package com.homesweet.homesweetback.domain.order.controller;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.order.dto.request.CreateOrderRequest;
import com.homesweet.homesweetback.domain.order.dto.request.PaymentConfirmRequest;
import com.homesweet.homesweetback.domain.order.dto.response.MyOrderItemResponse;
import com.homesweet.homesweetback.domain.order.dto.response.OrderDetailResponse;
import com.homesweet.homesweetback.domain.order.dto.response.OrderReadyResponse;
import com.homesweet.homesweetback.domain.order.dto.response.PaymentConfirmResponse;
import com.homesweet.homesweetback.domain.order.service.OrderService;
import com.homesweet.homesweetback.domain.order.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /*나의 주문 목록 조회 */
    @GetMapping
    public ResponseEntity<List<MyOrderItemResponse>> getMyOrders(
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ){
        if (principal == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = principal.getUserId();
        List<MyOrderItemResponse> myOrders = orderService.getMyOrders(userId);
        return ResponseEntity.ok(myOrders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(
            @PathVariable Long orderId,
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = principal.getUserId();

        OrderDetailResponse response = orderService.getOrderDetail(orderId, userId);
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

        PaymentConfirmResponse response = paymentService.confirmPayment(dto, userId);
        return ResponseEntity.ok(response);
    }
}