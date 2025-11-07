package com.homesweet.homesweetback.domain.product.data;

import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewResponse;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewStatisticsResponse;
import com.homesweet.homesweetback.domain.product.review.domain.ProductReview;

import java.time.LocalDateTime;
import java.util.Map;

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

    // 상품 리뷰 응답 DTO 생성
    public static ProductReviewResponse createReviewResponse(Long id, Long productId, Long userId, int rating, String comment) {
        return ProductReviewResponse.builder()
                .reviewId(id)
                .productId(productId)
                .userId(userId)
                .rating(rating)
                .comment(comment)
                .reviewImageUrl("https://s3.aws/review_" + id + ".jpg")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // 상품 리뷰 통계 응답 DTO 생성
    public static ProductReviewStatisticsResponse createReviewStatisticsResponse(Long productId) {
        return ProductReviewStatisticsResponse.of(
                productId,
                10L,
                4.5,
                Map.of(
                        5, 6L,
                        4, 3L,
                        3, 1L
                )
        );
    }

    // 아무 통계 정보가 없는 응답 DTO 생성
    public static ProductReviewStatisticsResponse createEmptyReviewStatisticsResponse(Long productId) {
        return ProductReviewStatisticsResponse.of(
                productId,
                0L,
                0.0,
                Map.of()
        );
    }
}
