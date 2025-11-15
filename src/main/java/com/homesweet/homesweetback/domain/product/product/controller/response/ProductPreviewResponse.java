package com.homesweet.homesweetback.domain.product.product.controller.response;

import com.homesweet.homesweetback.domain.product.product.domain.Product;
import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewStatisticsResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 제품 프리뷰 응답 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
public record ProductPreviewResponse(
        Long id,
        Long categoryId,
        Long sellerId,
        String name,
        String imageUrl,
        String brand,
        Integer basePrice,
        BigDecimal discountRate,
        String description,
        Integer shippingPrice,
        ProductStatus status,
        Double averageRating,
        Long reviewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProductPreviewResponse of(
            Product product,
            ProductReviewStatisticsResponse stats
    ) {
        return new ProductPreviewResponse(
                product.getId(),
                product.getCategoryId(),
                product.getSellerId(),
                product.getName(),
                product.getImageUrl(),
                product.getBrand(),
                product.getBasePrice(),
                product.getDiscountRate(),
                product.getDescription(),
                product.getShippingPrice(),
                product.getStatus(),
                stats.averageRating(),
                stats.totalCount(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

}
