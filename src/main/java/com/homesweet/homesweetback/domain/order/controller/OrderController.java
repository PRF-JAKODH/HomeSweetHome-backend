//package com.homesweet.homesweetback.domain.order.controller;
//
//import com.homesweet.homesweetback.domain.order.dto.request.CreateOrderRequest;
//import com.homesweet.homesweetback.domain.order.dto.request.PaymentConfirmRequest;
//import com.homesweet.homesweetback.domain.order.dto.response.OrderReadyResponse;
//import com.homesweet.homesweetback.domain.order.dto.response.PaymentConfirmResponse;
//import com.homesweet.homesweetback.domain.order.service.OrderService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//
//import org.springframework.web.bind.annotation.*;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
//import org.springframework.http.HttpStatus;
//
//@RestController // 이 클래스가 REST API 컨트롤러임을 선언
//@RequiredArgsConstructor // final OrderService 주입
//@RequestMapping("/api/v1/orders") // 이 컨트롤러의 모든 API는 "/api/v1/orders"로 시작
//public class OrderController {
//
//    private final OrderService orderService;
//
//     // API 1: 주문 생성 (결제 준비)
//     // POST /api/v1/orders
//     @PostMapping
//     public ResponseEntity<OrderReadyResponse> createOrder(
//             @Valid @RequestBody CreateOrderRequest dto,
//             @AuthenticationPrincipal OAuth2UserPrincipal principal // ★ 타입 명시
//     ) {
//         if (principal == null) {
//             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//         }
//         Long userId = principal.getUserId(); // ★ .getUserId() 메서드 호출
//
//         // --- Service 호출 ---
//         OrderReadyResponse response = orderService.createOrder(dto, userId); // ★ 실제 userId 전달
//         return ResponseEntity.ok(response);
//     }
//
//    /**
//     * API 2: 결제 검증 및 완료
//     * POST /api/v1/orders/payments/confirm
//     */
//    @PostMapping("/payments/confirm")
//    public ResponseEntity<PaymentConfirmResponse> confirmPayment(
//            @Valid @RequestBody PaymentConfirmRequest dto,
//            @AuthenticationPrincipal OAuth2UserPrincipal principal // ★ 타입 명시
//    ) {
//        // --- 실제 userId 추출 ---
//        if (principal == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//        Long userId = principal.getUserId(); // ★ .getUserId() 메서드 호출
//
//        // --- Service 호출 ---
//        // (OrderService의 confirmPayment 메서드 시그니처도 userId를 받도록 수정 필요)
//        PaymentConfirmResponse response = orderService.confirmPayment(dto, userId); // ★ 실제 userId 전달
//        return ResponseEntity.ok(response);
//    }
//}