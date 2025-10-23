package com.homesweet.homesweetback.domain.product.review.domain;

import java.time.LocalDateTime;

/**
 * 제품 리뷰 도메인
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
public record ProductReview(
        Long id,
        Long productId,
        Long userId,
        Integer rating,
        String comment,
        String imageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
