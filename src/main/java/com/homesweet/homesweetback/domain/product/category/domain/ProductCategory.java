package com.homesweet.homesweetback.domain.product.category.domain;

import java.time.LocalDateTime;

/**
 * 제품 카테고리 도메인
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
public record ProductCategory(
        Long id,
        String name,
        Long parentId,
        Integer depth,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
