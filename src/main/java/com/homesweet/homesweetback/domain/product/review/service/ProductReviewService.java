package com.homesweet.homesweetback.domain.product.review.service;

import com.homesweet.homesweetback.domain.product.review.controller.request.ProductReviewCreateRequest;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewResponse;

/**
 * 제품 리뷰 서비스 명세
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
public interface ProductReviewService {

    ProductReviewResponse createReview(Long productId, Long userId, ProductReviewCreateRequest request);
}
