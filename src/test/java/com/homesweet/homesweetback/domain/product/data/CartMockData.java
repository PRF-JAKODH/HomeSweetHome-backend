package com.homesweet.homesweetback.domain.product.data;

import com.homesweet.homesweetback.domain.product.cart.controller.response.CartResponse;
import com.homesweet.homesweetback.domain.product.cart.domain.Cart;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 장바구니 Mock 데이터
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 8.
 */
public class CartMockData {

    // 장바구니 Mock 객체 생성
    public static Cart createMockCart(Long userId, Long skuId) {
        return Cart.builder()
                .id(1L)
                .userId(userId)
                .skuId(skuId)
                .quantity(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static CartResponse createCartResponse(
            Long id,
            Long skuId,
            Long productId,
            String brand,
            String productName,
            String optionSummary,
            Integer basePrice,
            BigDecimal discountRate,
            Integer finalPrice,
            Integer shippingPrice,
            Integer quantity,
            String imageUrl
    ) {
        return CartResponse.builder()
                .id(id)
                .skuId(skuId)
                .productId(productId)
                .brand(brand)
                .productName(productName)
                .optionSummary(optionSummary)
                .basePrice(basePrice)
                .discountRate(discountRate)
                .finalPrice(finalPrice)
                .shippingPrice(shippingPrice)
                .quantity(quantity)
                .totalPrice(finalPrice * quantity + shippingPrice) // 단순 계산 로직
                .imageUrl(imageUrl)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .priceAdjustment(0)
                .build();
    }

    /**
     * 기본 장바구니 아이템 Mock (테스트용 편의 메서드)
     */
    public static CartResponse createDefaultCartResponse(Long id) {
        return createCartResponse(
                id,
                100L + id,
                200L + id,
                "홈스윗",
                "테스트상품" + id,
                "색상: 화이트 / 사이즈: L",
                30000,
                new BigDecimal("10.00"),
                27000,
                3000,
                1,
                "https://s3.aws/test" + id + ".jpg"
        );
    }
}
