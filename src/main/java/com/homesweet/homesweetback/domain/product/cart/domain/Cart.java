package com.homesweet.homesweetback.domain.product.cart.domain;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 장바구니 도메인
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
@Builder
public record Cart(
        Long id,
        Long userId,
        Long skuId,
        Integer quantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static Cart create(Long userId, Long skuId, Integer quantity) {
        return Cart.builder()
                .userId(userId)
                .skuId(skuId)
                .quantity(quantity)
                .build();
    }

    public Cart updateQuantity(Integer quantity) {
        return Cart.builder()
                .id(this.id)
                .userId(this.userId)
                .skuId(this.skuId)
                .quantity(quantity)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
