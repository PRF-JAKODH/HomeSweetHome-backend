package com.homesweet.homesweetback.domain.product.product.command.controller.response;

import com.homesweet.homesweetback.domain.product.product.command.domain.Product;
import com.homesweet.homesweetback.domain.product.product.command.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.product.query.repository.document.ProductDocument;
import com.homesweet.homesweetback.domain.product.review.domain.ProductReviewStatistics;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 제품 프리뷰 응답 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
@Builder
public record ProductPreviewResponse(
        Long id,
        Long categoryId,
        Long sellerId,
        String name,
        String imageUrl,
        String brand,
        Integer basePrice,
        BigDecimal discountRate,
        Integer shippingPrice,
        ProductStatus status,
        Double averageRating,
        Long reviewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProductPreviewResponse of(
            Product product,
            ProductReviewStatistics stats
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
                product.getShippingPrice(),
                product.getStatus(),
                stats.averageRating(),
                stats.totalCount(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public static ProductPreviewResponse fromDocument(ProductDocument document) {
        return ProductPreviewResponse.builder()
                .id(document.getProductId())
                .categoryId(document.getCategoryId())
                .name(document.getName())
                .imageUrl(document.getImageUrl())
                .brand(document.getBrand())
                .basePrice(document.getBasePrice())
                .discountRate(document.getDiscountRate() != null ? BigDecimal.valueOf(document.getDiscountRate()) : BigDecimal.ZERO)
                .shippingPrice(document.getShippingPrice())
                .status(ProductStatus.valueOf(document.getStatus()))
                .averageRating(document.getAverageRating())
                .reviewCount(document.getReviewCount())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
