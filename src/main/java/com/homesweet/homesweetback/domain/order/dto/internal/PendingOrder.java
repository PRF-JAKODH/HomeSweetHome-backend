package com.homesweet.homesweetback.domain.order.dto.internal;

import java.util.List;

public record PendingOrder(
        Long userId,
        String orderNumber,
        long totalAmount,
        List<PendingOrderItem> items,
        String recipientName,
        String recipientPhone,
        String shippingAddress,
        String shippingRequest
) {
    public record PendingOrderItem(
            Long skuId,
            int quantity,
            long price
    ) {}
}