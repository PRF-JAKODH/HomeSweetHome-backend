package com.homesweet.homesweetback.domain.product.review.controller.response;

import java.util.Map;

/**
 * 제품 리뷰 통계 응답 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 25.
 */
public record ProductReviewStatisticsResponse(
        Long productId,
        Long totalCount,
        Double averageRating,
        Map<Integer, Long> ratingCounts
) {
    public static ProductReviewStatisticsResponse of(
            Long productId,
            Long totalCount,
            Double averageRating,
            Map<Integer, Long> ratingCounts
    ) {
        return new ProductReviewStatisticsResponse(productId, totalCount, averageRating, ratingCounts);
    }
}
