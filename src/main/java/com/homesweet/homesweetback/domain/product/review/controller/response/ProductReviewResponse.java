package com.homesweet.homesweetback.domain.product.review.controller.response;

import com.homesweet.homesweetback.domain.product.review.domain.ProductReview;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 제품 리뷰 응답 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
@Builder
public record ProductReviewResponse(
        Long reviewId,
        Long productId,
        Long userId,
        String productName,
        String username,
        Integer rating,
        String comment,
        String imageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProductReviewResponse from(ProductReview domain) {
        return ProductReviewResponse.builder()
                .reviewId(domain.id())
                .productId(domain.productId())
                .userId(domain.userId())
                .rating(domain.rating())
                .comment(domain.comment())
                .imageUrl(domain.imageUrl())
                .createdAt(domain.createdAt())
                .updatedAt(domain.updatedAt())
                .build();
    }
}
