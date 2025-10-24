package com.homesweet.homesweetback.domain.product.review.controller;

import com.homesweet.homesweetback.common.util.ScrollResponse;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.product.review.controller.request.ProductReviewCreateRequest;
import com.homesweet.homesweetback.domain.product.review.controller.request.ProductReviewUpdateRequest;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewResponse;
import com.homesweet.homesweetback.domain.product.review.service.ProductReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 제품 리뷰 컨트롤러
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
@RestController
@RequestMapping("/api/v1/product/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService service;

    @PostMapping("/{productId}")
    public ResponseEntity<ProductReviewResponse> createReview(
            @RequestHeader(value = "X-Test-User-Id", defaultValue = "1") Long userId, // 테스트 용
            @PathVariable Long productId,
            @Valid @ModelAttribute ProductReviewCreateRequest request) {

        ProductReviewResponse response = service.createReview(productId, userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 특정 상품의 리뷰를 전체 조회합니다
    @GetMapping("/{productId}")
    public ResponseEntity<ScrollResponse<ProductReviewResponse>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") int limit) {

        ScrollResponse<ProductReviewResponse> response = service.getProductReviews(productId, cursorId, limit);
        return ResponseEntity.ok(response);

    }

    // 특정 사용자가 등록한 리뷰를 전체 조회합니다
    @GetMapping("/me")
    public ResponseEntity<ScrollResponse<ProductReviewResponse>> getMyReviews(
            @RequestHeader(value = "X-Test-User-Id", defaultValue = "1") Long userId, // 테스트 용
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        ScrollResponse<ProductReviewResponse> response = service.getUserReviews(userId, cursorId, limit);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<ProductReviewResponse> updateReview(
            @RequestHeader(value = "X-Test-User-Id", defaultValue = "1") Long userId, // 테스트 용
            @PathVariable Long reviewId,
            @ModelAttribute ProductReviewUpdateRequest request
    ) {

        ProductReviewResponse response = service.updateReview(reviewId, userId, request);

        return ResponseEntity.ok(response);
    }
}
