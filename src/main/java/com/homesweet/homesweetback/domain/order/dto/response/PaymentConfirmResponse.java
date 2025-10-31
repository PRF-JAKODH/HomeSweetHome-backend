package com.homesweet.homesweetback.domain.order.dto.response;

public record PaymentConfirmResponse(
        Long orderId, // ★ String merchantUid -> Long orderId
        String status
) {
}