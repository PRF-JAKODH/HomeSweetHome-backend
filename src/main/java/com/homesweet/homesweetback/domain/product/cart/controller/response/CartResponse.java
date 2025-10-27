package com.homesweet.homesweetback.domain.product.cart.controller.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 장바구니 응답 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
@Builder
public record CartResponse(
        Long id,
        Long skuId,
        String brand,
        String productName,
        String optionSummary,
        Integer basePrice,
        BigDecimal discountRate,
        Integer finalPrice,
        Integer shippingPrice,
        Integer quantity,
        Integer totalPrice,
        String imageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

}
