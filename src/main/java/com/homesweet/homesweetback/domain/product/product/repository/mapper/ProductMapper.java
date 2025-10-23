package com.homesweet.homesweetback.domain.product.product.repository.mapper;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.product.domain.*;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 제품 도메인 <-> 엔티티 변환 클래스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
@Component
public class ProductMapper {

    /**
     * Entity -> Domain 변환
     */
    public Product toDomain(ProductEntity entity) {
        return Product.builder()
                .id(entity.getId())
                .categoryId(entity.getCategory().getId())
                .sellerId(entity.getSeller().getId())
                .name(entity.getName())
                .imageUrl(entity.getImageUrl())
                .brand(entity.getBrand())
                .basePrice(entity.getBasePrice())
                .discountRate(entity.getDiscountRate())
                .description(entity.getDescription())
                .shippingPrice(entity.getShippingPrice())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .detailImages(entity.getDetailImages().stream()
                        .map(img -> ProductDetailImage.builder()
                                .id(img.getId())
                                .imageUrl(img.getImageUrl())
                                .createdAt(img.getCreatedAt())
                                .updatedAt(img.getUpdatedAt())
                                .build())
                        .toList())
                .optionGroups(entity.getOptionGroups().stream()
                        .map(group -> ProductOptionGroup.builder()
                                .id(group.getId())
                                .groupName(group.getGroupName())
                                .values(group.getValues().stream()
                                        .map(value -> ProductOptionValue.builder()
                                                .id(value.getId())
                                                .value(value.getValue())
                                                .createdAt(value.getCreatedAt())
                                                .build())
                                        .toList())
                                .createdAt(group.getCreatedAt())
                                .build())
                        .toList())
                .skus(entity.getSkus().stream()
                        .map(sku -> Sku.builder()
                                .id(sku.getId())
                                .priceAdjustment(sku.getPriceAdjustment())
                                .stockQuantity(sku.getStockQuantity())
                                .createdAt(sku.getCreatedAt())
                                .updatedAt(sku.getUpdatedAt())
                                .optionValueIndexes(
                                        sku.getSkuOptions().stream()
                                                .map(link -> link.getOptionValue().getId())
                                                .toList()
                                )
                                .build())
                        .toList())
                .build();
    }

    /**
     * Domain -> Entity 변환 (인덱스 기반)
     */
    public ProductEntity toEntity(Product domain) {
        // Product Entity 생성
        ProductEntity entity = ProductEntity.builder()
                .category(ProductCategoryEntity.builder().id(domain.getCategoryId()).build())
                .seller(User.builder().id(domain.getSellerId()).build())
                .name(domain.getName())
                .imageUrl(domain.getImageUrl())
                .brand(domain.getBrand())
                .basePrice(domain.getBasePrice())
                .discountRate(domain.getDiscountRate())
                .description(domain.getDescription())
                .shippingPrice(domain.getShippingPrice())
                .status(domain.getStatus())
                .build();

        // 상세 이미지 추가
        if (domain.getDetailImages() != null) {
            domain.getDetailImages().forEach(img ->
                    entity.addDetailImage(
                            ProductDetailImageEntity.builder()
                                    .imageUrl(img.getImageUrl())
                                    .build()
                    )
            );
        }

        // 옵션 그룹 추가 (Cascade로 옵션 값도 함께 저장됨)
        if (domain.getOptionGroups() != null) {
            domain.getOptionGroups().forEach(group ->
                    entity.addOption(
                            ProductOptionGroupEntity.builder()
                                    .groupName(group.getGroupName())
                                    .values(
                                            group.getValues().stream()
                                                    .map(value -> ProductOptionValueEntity.builder()
                                                            .value(value.getValue())
                                                            .build())
                                                    .toList()
                                    )
                                    .build()
                    )
            );
        }

        // SKU 추가 - 이미 entity에 추가된 옵션을 인덱스로 참조
        if (domain.getSkus() != null && !domain.getSkus().isEmpty()) {
            // entity에 추가된 모든 옵션 값을 flat하게 가져오기
            List<ProductOptionValueEntity> allOptionValues = entity.getOptionGroups().stream()
                    .flatMap(group -> group.getValues().stream())
                    .toList();

            domain.getSkus().forEach(sku -> {
                SkuEntity skuEntity = SkuEntity.builder()
                        .priceAdjustment(sku.getPriceAdjustment())
                        .stockQuantity(sku.getStockQuantity())
                        .build();

                entity.addSku(skuEntity);

                // 인덱스를 사용하여 실제 저장될 옵션 값 엔티티 참조
                if (sku.getOptionValueIndexes() != null && !sku.getOptionValueIndexes().isEmpty()) {
                    sku.getOptionValueIndexes().forEach(index -> {
                        int idx = index.intValue();

                        if (idx < 0 || idx >= allOptionValues.size()) {
                            throw new ProductException(ErrorCode.OUT_OF_OPTION_INDEX);
                        }

                        ProductOptionValueEntity optionValue = allOptionValues.get(idx);

                        ProductSkuOptionEntity link = ProductSkuOptionEntity.builder()
                                .sku(skuEntity)
                                .optionValue(optionValue)
                                .build();
                        skuEntity.addSkuOption(link);
                    });
                }
            });
        }

        return entity;
    }
}
