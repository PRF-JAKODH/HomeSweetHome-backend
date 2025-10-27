package com.homesweet.homesweetback.domain.product.product.controller.response;

import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 제품 상세 응답 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 25.
 */
@Builder
public record ProductDetailResponse(
        Long id,
        Long categoryId,
        Long sellerId,
        String name,
        String imageUrl,
        List<String> detailImageUrls,
        String brand,
        Integer basePrice,
        BigDecimal discountRate,
        Integer discountedPrice,
        String description,
        Integer shippingPrice,
        ProductStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
