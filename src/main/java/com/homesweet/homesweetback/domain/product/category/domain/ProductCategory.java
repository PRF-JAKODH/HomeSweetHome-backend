package com.homesweet.homesweetback.domain.product.category.domain;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 제품 카테고리 도메인
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@Builder
public record ProductCategory(
        Long id,
        String name,
        Long parentId,
        Integer depth,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static final int MAX_DEPTH = 2;

    public static ProductCategory createCategory(String name, Long parentId, Integer depth) {
        return ProductCategory.builder()
                .name(name)
                .parentId(parentId)
                .depth(depth)
                .build();
    }
}
