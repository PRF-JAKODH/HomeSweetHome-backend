package com.homesweet.homesweetback.domain.product.review.domain;

import com.homesweet.homesweetback.domain.product.review.controller.request.ProductReviewCreateRequest;
import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 제품 리뷰 도메인
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
@Builder
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

    public static ProductReview create(Long productId, Long userId, Integer rating, String comment, String imageUrl) {
        return ProductReview.builder()
                .productId(productId)
                .userId(userId)
                .rating(rating)
                .comment(comment)
                .imageUrl(imageUrl)
                .build();
    }

    public ProductReview update(Integer rating, String comment, String imageUrl) {
        return ProductReview.builder()
                .id(this.id)
                .productId(this.productId)
                .userId(this.userId)
                .rating(rating != null ? rating : this.rating)
                .comment(comment != null ? comment : this.comment)
                .imageUrl(imageUrl != null ? imageUrl : this.imageUrl)
                .createdAt(this.createdAt)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
