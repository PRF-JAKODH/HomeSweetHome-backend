package com.homesweet.homesweetback.domain.product.review.repository.jpa.querydsl;

import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewResponse;

import java.util.List;

/**
 * 상품 리뷰 QueryDSL 인터페이스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
public interface CustomProductReviewRepository {

    List<ProductReviewResponse> findNextReviews(Long productId, Long cursorId, int size);

    List<ProductReviewResponse> findNextUserReviews(Long userId, Long cursorId, int limit);
}
