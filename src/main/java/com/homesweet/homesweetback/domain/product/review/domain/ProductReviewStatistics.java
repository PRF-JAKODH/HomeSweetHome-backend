package com.homesweet.homesweetback.domain.product.review.domain;

/**
 * 상품 통계 정보
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 15.
 */
public record ProductReviewStatistics(
        Long productId,
        Long totalCount,
        Double averageRating
) {

    public static ProductReviewStatistics empty() {
        return new ProductReviewStatistics(null, 0L, 0.0);
    }
}
