package com.homesweet.homesweetback.domain.product.product.command.controller.response;

import com.homesweet.homesweetback.domain.product.product.command.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.ProductEntity;
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
    public static ProductDetailResponse from(ProductEntity entity, List<String> detailImageUrls) {
        return ProductDetailResponse.builder()
                .id(entity.getId())
                .categoryId(entity.getCategory().getId())
                .sellerId(entity.getSeller().getId())
                .name(entity.getName())
                .imageUrl(entity.getImageUrl())
                .detailImageUrls(detailImageUrls)
                .brand(entity.getBrand())
                .basePrice(entity.getBasePrice())
                .discountRate(entity.getDiscountRate())
                .discountedPrice(calculateDiscountedPrice(entity))
                .description(entity.getDescription())
                .shippingPrice(entity.getShippingPrice())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static Integer calculateDiscountedPrice(ProductEntity entity) {
        if (entity.getBasePrice() == null || entity.getDiscountRate() == null)
            return null;

        double discountRate = entity.getDiscountRate().doubleValue();
        return (int) Math.round(entity.getBasePrice() * (1 - discountRate / 100));
    }
}
