package com.homesweet.homesweetback.domain.product.review.repository;

import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewResponse;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewStatisticsResponse;
import com.homesweet.homesweetback.domain.product.review.domain.ProductReview;

import java.util.List;
import java.util.Optional;

/**
 * 제품 리뷰 레포 명세
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
public interface ProductReviewRepository {

    ProductReview save(ProductReview domain);

    Optional<ProductReview> findById(Long id);

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    List<ProductReviewResponse> findNextReviews(Long productId, Long cursorId, int size);

    List<ProductReviewResponse> findNextUserReviews(Long userId, Long cursorId, int limit);

    ProductReview update(ProductReview domain);

    ProductReviewStatisticsResponse getReviewStatistics(Long productId);
}
