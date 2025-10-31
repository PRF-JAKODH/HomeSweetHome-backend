package com.homesweet.homesweetback.domain.product.category.repository.mapper;

import com.homesweet.homesweetback.domain.product.category.domain.ProductCategory;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import org.springframework.stereotype.Component;

/**
 * 제품 카테고리 도메인 <-> 엔티티 변환 클래스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@Component
public class ProductCategoryMapper {

    // Entity -> Domain
    public ProductCategory toDomain(ProductCategoryEntity entity) {
        if (entity == null) {
            return null;
        }

        return ProductCategory.builder()
                .id(entity.getId())
                .name(entity.getName())
                .parentId(entity.getParentId())
                .depth(entity.getDepth())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // Domain -> Entity
    public ProductCategoryEntity toEntity(ProductCategory domain) {
        if (domain == null) {
            return null;
        }

        return ProductCategoryEntity.builder()
                .name(domain.name())
                .parentId(domain.parentId())
                .depth(domain.depth())
                .build();
    }
}
