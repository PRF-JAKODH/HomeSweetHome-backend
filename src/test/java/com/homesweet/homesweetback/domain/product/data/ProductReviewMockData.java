package com.homesweet.homesweetback.domain.product.data;

import com.homesweet.homesweetback.domain.product.review.domain.ProductReview;

import java.time.LocalDateTime;

/**
 * 상품 리뷰 관련 Mock 객체 생성
 *
 * @author junnukim1007gmail.com
 */
public class ProductReviewMockData {

    // 상품 리뷰 생성
    public static ProductReview createMockReview(Long productId, Long userId, Integer rating, String comment, String imageUrl) {
        return ProductReview.builder()
                .id(10L)
                .productId(productId)
                .userId(userId)
                .rating(rating)
                .comment(comment)
                .imageUrl(imageUrl)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
