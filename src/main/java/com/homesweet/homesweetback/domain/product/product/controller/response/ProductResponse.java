package com.homesweet.homesweetback.domain.product.product.controller.response;

import com.homesweet.homesweetback.domain.product.product.domain.Product;
import com.homesweet.homesweetback.domain.product.product.domain.ProductDetailImage;
import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 제품 응답 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
public record ProductResponse(
        Long id,
        Long categoryId,
        Long sellerId,
        String name,
        String imageUrl,
        String brand,
        Integer basePrice,
        BigDecimal discountRate,
        Integer discountedPrice,
        String description,
        Integer shippingPrice,
        ProductStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> detailImageUrls,
        List<ProductOptionGroupResponse> optionGroups,
        List<SkuResponse> skus
) {

    /**
     * 제품 옵션 응답 DTO
     */
    public record ProductOptionGroupResponse(
            Long id,
            String groupName,
            List<ProductOptionValueResponse> values
    ) {}

    public record ProductOptionValueResponse(
            Long id,
            String value
    ) {}

    /**
     * SKU 응답 DTO
     */
    public record SkuResponse(
            Long id,
            Integer priceAdjustment,
            Integer stockQuantity,
            Integer finalPrice,
            boolean inStock,
            List<Long> optionValueIds
    ) {}

    public static ProductResponse from(Product domain) {
        // 상세 이미지 URL 리스트 추출
        List<String> detailImageUrls = domain.getDetailImages().stream()
                .map(ProductDetailImage::getImageUrl)
                .toList();

        // 옵션 응답 변환
        List<ProductResponse.ProductOptionGroupResponse> optionGroupResponses = domain.getOptionGroups().stream()
                .map(group -> new ProductResponse.ProductOptionGroupResponse(
                        group.getId(),
                        group.getGroupName(),
                        group.getValues().stream()
                                .map(value -> new ProductOptionValueResponse(value.getId(), value.getValue()))
                                .toList()
                ))
                .toList();

        // SKU 응답 변환
        List<ProductResponse.SkuResponse> skuResponses = domain.getSkus().stream()
                .map(sku -> new ProductResponse.SkuResponse(
                        sku.getId(),
                        sku.getPriceAdjustment(),
                        sku.getStockQuantity(),
                        domain.getDiscountedPrice() + sku.getPriceAdjustment(),
                        sku.isInStock(),
                        sku.getOptionValueIds()
                ))
                .toList();

        return new ProductResponse(
                domain.getId(),
                domain.getCategoryId(),
                domain.getSellerId(),
                domain.getName(),
                domain.getImageUrl(),
                domain.getBrand(),
                domain.getBasePrice(),
                domain.getDiscountRate(),
                domain.getDiscountedPrice(),
                domain.getDescription(),
                domain.getShippingPrice(),
                domain.getStatus(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                detailImageUrls,
                optionGroupResponses,
                skuResponses
        );
    }
}