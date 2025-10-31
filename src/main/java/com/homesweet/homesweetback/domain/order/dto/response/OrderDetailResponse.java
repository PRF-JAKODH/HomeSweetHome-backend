package com.homesweet.homesweetback.domain.order.dto.response;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.Payment;
import lombok.Builder;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

// "주문 상세" 조회용 DTO
@Builder
public record OrderDetailResponse(
        // --- 1. 주문 기본 정보 ---
        Long orderId,
        String orderNumber,
        String orderDate,
        String orderStatus,
        String deliveryStatus,

        // --- 2. 주문자 정보 (User) ---
        String customerName,
        String customerPhone,
        String customerEmail,

        // --- 3. 배송지 정보 (Order 또는 User에서 가져옴) ---
        // (Order 엔티티에 배송지 필드가 없으므로 User 필드 사용)
        String shippingAddress,

        // --- 4. 결제 정보 (Payment) ---
        String paymentMethod,
        Long totalAmount, // 최종 결제 금액 (상품 총액 + 배송비)
        Integer totalShippingPrice, // 총 배송비
        Long usedPoint, // (Order 엔티티에 used_point가 없으므로 Payment 또는 Order에서 가져와야 함)

        // --- 5. 주문 상품 목록 (OrderItem) ---
        List<OrderDetailItemResponse> orderItems
) {

    // (정적 팩토리 메서드)
    // Order, Payment, User 엔티티를 조합하여 DTO를 생성
    public static OrderDetailResponse of(Order order, Payment payment, User user, Integer totalShippingPrice) {

        List<OrderDetailItemResponse> itemDetails = order.getOrderItems().stream()
                .map(OrderDetailItemResponse::from)
                .collect(Collectors.toList());

        // payment가 null인 경우(주문 생성만 하고 결제하지 않은 상태)
        String paymentMethod = (payment != null) ? payment.getMethod() : "결제 대기중";
        Long totalAmount = (payment != null) ? payment.getAmount() : order.getTotalAmount(); // payment가 없으면 Order의 총액 사용
        Long usedPoint = 0L; // (TODO: usedPoint 로직)

        return OrderDetailResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.generateOrderNumber())
                .orderDate(order.getOrderedAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .orderStatus(order.getOrderStatus().name())
                .deliveryStatus(order.getDeliveryStatus().name())
                .customerName(user.getName())
                .customerPhone(user.getPhoneNumber())
                .customerEmail(user.getEmail())
                .shippingAddress(user.getAddress()) // User의 주소를 배송지로 사용
                .paymentMethod(payment.getMethod())
                .totalAmount(payment.getAmount())
                .totalShippingPrice(totalShippingPrice) // (OrderService에서 계산 필요)
                .usedPoint(0L) // (TODO: Order 엔티티에 used_point가 없으므로 임시 0)
                .orderItems(itemDetails)
                .build();
    }
}