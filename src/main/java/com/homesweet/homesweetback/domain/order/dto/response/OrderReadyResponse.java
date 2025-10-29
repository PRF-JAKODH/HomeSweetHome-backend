package com.homesweet.homesweetback.domain.order.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record OrderReadyResponse(
        Long orderId,
        String orderNumber,
        String username,
        String address,
        String phoneNumber,
        List<OrderItemDetail> orderItems,
        Long totalAmount,
        Integer totalShippingPrice
) {
    public record OrderItemDetail(
            Long orderItemId,
            String imageUrl,
            String brand,
            String productName,
            String optionName,
            Integer basePrice,
            BigDecimal discountRate,
            Integer shippingPrice,
            Long finalPrice,
            Integer quantity
    ) {}
}