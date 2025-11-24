package com.homesweet.homesweetback.domain.product.product.command.controller.response;

import com.homesweet.homesweetback.domain.product.product.command.domain.ProductStatus;
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
        String categoryPath,
        Integer basePrice,
        BigDecimal discountRate,
        Integer shippingPrice,
        Long totalStock,
        ProductStatus status,
        LocalDateTime createdAt
) {
}
