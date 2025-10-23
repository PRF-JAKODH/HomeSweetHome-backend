package com.homesweet.homesweetback.domain.product.review.controller;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.product.review.controller.request.ProductReviewCreateRequest;
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
            @PathVariable Long productId,
            @Valid @ModelAttribute ProductReviewCreateRequest request) {

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Long userId = principal.getUserId();

        ProductReviewResponse response = service.createReview(productId, userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
