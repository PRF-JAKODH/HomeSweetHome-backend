package com.homesweet.homesweetback.domain.order.controller;

import com.homesweet.homesweetback.domain.order.dto.CreateOrderRequest;
import com.homesweet.homesweetback.domain.order.dto.OrderResponse;
import com.homesweet.homesweetback.domain.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 주문 API 컨트롤러
 *
 * 주문 흐름:
 * 1. POST /api/v1/orders - 장바구니에서 주문 생성
 * 2. 클라이언트에서 토스페이먼츠 결제창 호출 (orderNumber, totalAmount 사용)
 * 3. 결제 완료 후 POST /api/v1/payments/confirm 호출
 *
 * TODO: 테스트 완료 후 인증 로직 원복 필요
 */
@Tag(name = "Order", description = "주문 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // TODO: 테스트 완료 후 원복 - 테스트용 하드코딩 userId
    private static final Long TEST_USER_ID = 1L;

    /**
     * 주문 생성 API
     * 장바구니에서 선택한 상품들로 주문을 생성합니다.
     * 생성된 주문의 orderNumber, totalAmount로 토스페이먼츠 결제창을 호출하세요.
     */
    @Operation(summary = "주문 생성", description = "장바구니에서 주문 생성. 결제 전 단계.")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        log.info("주문 생성 API 호출: userId={}", TEST_USER_ID);
        OrderResponse response = orderService.createFromCart(TEST_USER_ID, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 주문 목록 조회 API
     */
    @Operation(summary = "내 주문 목록", description = "로그인한 사용자의 주문 목록 조회")
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders() {

        log.info("주문 목록 조회 API 호출: userId={}", TEST_USER_ID);
        List<OrderResponse> orders = orderService.getMyOrders(TEST_USER_ID);
        return ResponseEntity.ok(orders);
    }

    /**
     * 주문 상세 조회 API
     */
    @Operation(summary = "주문 상세 조회", description = "주문 ID로 상세 정보 조회")
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long orderId) {

        log.info("주문 상세 조회 API 호출: userId={}, orderId={}", TEST_USER_ID, orderId);
        OrderResponse response = orderService.getOrder(orderId, TEST_USER_ID);
        return ResponseEntity.ok(response);
    }

    /**
     * 주문 취소 API
     * 결제 전 PENDING 상태에서만 취소 가능
     */
    @Operation(summary = "주문 취소", description = "결제 전 주문 취소 (PENDING 상태에서만 가능)")
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long orderId) {

        log.info("주문 취소 API 호출: userId={}, orderId={}", TEST_USER_ID, orderId);
        orderService.cancelOrder(orderId, TEST_USER_ID);
        return ResponseEntity.noContent().build();
    }
}

