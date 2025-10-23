package com.homesweet.homesweetback.domain.product.product.domain;

import com.homesweet.homesweetback.domain.product.category.domain.ProductCategory;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductCreateRequest;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 제품 도메인 모델
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
@Getter
@Builder
public class Product {

    private Long id;
    private Long categoryId;
    private Long sellerId;
    private String name;
    private String imageUrl;
    private String brand;
    private Integer basePrice;
    private BigDecimal discountRate;
    private String description;
    private Integer shippingPrice;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<ProductDetailImage> detailImages = new ArrayList<>();

    @Builder.Default
    private List<ProductOptionGroup> optionGroups = new ArrayList<>();

    @Builder.Default
    private List<Sku> skus = new ArrayList<>();

    /**
     * 할인된 가격 계산
     */
    public Integer getDiscountedPrice() {
        if (discountRate == null || discountRate.compareTo(BigDecimal.ZERO) == 0) {
            return basePrice;
        }
        BigDecimal discount = BigDecimal.valueOf(basePrice)
                .multiply(discountRate)
                .divide(BigDecimal.valueOf(100));
        return basePrice - discount.intValue();
    }

    public static Product createProduct(
            Long categoryId,
            Long sellerId,
            String name,
            String imageUrl,
            String brand,
            Integer basePrice,
            BigDecimal discountRate,
            String description,
            Integer shippingPrice,
            List<ProductDetailImage> detailImages,
            List<ProductOptionGroup> optionGroups,
            List<Sku> skus
    ) {
        return Product.builder()
                .categoryId(categoryId)
                .sellerId(sellerId)
                .name(name)
                .imageUrl(imageUrl)
                .brand(brand)
                .basePrice(basePrice)
                .discountRate(discountRate != null ? discountRate : BigDecimal.ZERO)
                .description(description)
                .shippingPrice(shippingPrice)
                .status(ProductStatus.ON_SALE)
                .detailImages(detailImages != null ? detailImages : new ArrayList<>())
                .optionGroups(optionGroups)
                .skus(skus != null ? skus : new ArrayList<>())
                .build();
    }
}