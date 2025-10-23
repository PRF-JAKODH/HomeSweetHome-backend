package com.homesweet.homesweetback.domain.product.review.controller.response;

import java.time.LocalDateTime;

/**
 * 제품 리뷰 응답 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
public record ProductReviewResponse(
        Long reviewId,
        Long productId,
        Long userId,
        Integer rating,
        String comment,
        String imageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
