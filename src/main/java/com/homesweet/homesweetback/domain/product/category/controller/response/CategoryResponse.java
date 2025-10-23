package com.homesweet.homesweetback.domain.product.category.controller.response;

import com.homesweet.homesweetback.domain.product.category.domain.ProductCategory;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 제품 카테고리 응답 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@Builder
public record CategoryResponse(
        Long id,
        String name,
        Long parentId,
        Integer depth,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CategoryResponse from(ProductCategory domain) {
        return CategoryResponse.builder()
                .id(domain.id())
                .name(domain.name())
                .parentId(domain.parentId())
                .depth(domain.depth())
                .createdAt(domain.createdAt())
                .updatedAt(domain.updatedAt())
                .build();
    }
}
