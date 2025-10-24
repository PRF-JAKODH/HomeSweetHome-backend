package com.homesweet.homesweetback.domain.product.product.controller.response;

import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 제품 관리 목록 조회 응답 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
@Builder
public record ProductManageResponse(
        Long id,
        String name,
        String imageUrl,
        // "가구 > 거실가구 > 소파 형식으로 조회되어야 한다!
        String categoryPath,
        Integer basePrice,
        BigDecimal discountRate,
        Integer shippingPrice,
        Long totalStock,
        ProductStatus status,
        LocalDateTime createdAt
) {
}
