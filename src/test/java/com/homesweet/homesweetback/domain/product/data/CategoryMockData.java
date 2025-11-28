package com.homesweet.homesweetback.domain.product.data;

import com.homesweet.homesweetback.domain.product.category.domain.ProductCategory;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;

import java.time.LocalDateTime;

/**
 * 카테고리 Mock 데이터
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 8.
 */
public class CategoryMockData {

    // 최상위 카테고리 (depth 0)
    public static ProductCategory createTopCategory(Long id, String name) {
        return ProductCategory.builder()
                .id(id)
                .name(name)
                .parentId(null)
                .depth(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // 중간 카테고리 (depth 1)
    public static ProductCategory createMidCategory(Long id, String name, Long parentId) {
        return ProductCategory.builder()
                .id(id)
                .name(name)
                .parentId(parentId)
                .depth(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // 하위 카테고리 (depth 2)
    public static ProductCategory createSubCategory(Long id, String name, Long parentId) {
        return ProductCategory.builder()
                .id(id)
                .name(name)
                .parentId(parentId)
                .depth(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static ProductCategoryEntity createCategoryEntity(Long id, String name, Long parentId, int depth) {
        return ProductCategoryEntity.builder()
                .id(id)
                .name(name)
                .parentId(parentId)
                .depth(depth)
                .build();
    }
}
