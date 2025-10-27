package com.homesweet.homesweetback.domain.product.product.repository.mapper;

import com.homesweet.homesweetback.domain.product.product.domain.Sku;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import org.springframework.stereotype.Component;

/**
 * 옵션 도메인 <-> 엔티티 매핑
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
@Component
public class SkuMapper {

    // Entity -> Domain
    public Sku toDomain(SkuEntity entity) {

        return Sku.builder()
                .id(entity.getId())
                .priceAdjustment(entity.getPriceAdjustment())
                .stockQuantity(entity.getStockQuantity())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // Domain -> Entity
    public SkuEntity toEntity(Sku domain) {

        return SkuEntity.builder()
                .priceAdjustment(domain.getPriceAdjustment())
                .stockQuantity(domain.getStockQuantity())
                .stockQuantity(domain.getStockQuantity())
                .build();

    }
}
