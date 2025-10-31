package com.homesweet.homesweetback.domain.product.review.service.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.util.ScrollResponse;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.product.cart.controller.response.CartResponse;
import com.homesweet.homesweetback.domain.product.product.domain.Product;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.repository.ProductRepository;
import com.homesweet.homesweetback.domain.product.product.repository.util.ProductImageUploader;
import com.homesweet.homesweetback.domain.product.review.controller.ProductReviewController;
import com.homesweet.homesweetback.domain.product.review.controller.request.ProductReviewCreateRequest;
import com.homesweet.homesweetback.domain.product.review.controller.request.ProductReviewUpdateRequest;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewResponse;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewStatisticsResponse;
import com.homesweet.homesweetback.domain.product.review.domain.ProductReview;
import com.homesweet.homesweetback.domain.product.review.repository.ProductReviewRepository;
import com.homesweet.homesweetback.domain.product.review.service.ProductReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 제품 리뷰 서비스 구현 코드
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
@Service
@RequiredArgsConstructor
public class ProductReviewServiceImpl implements ProductReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;
    private final ProductImageUploader imageUploader;

    @Override
    @Transactional
    public ProductReviewResponse createReview(Long productId, Long userId, ProductReviewCreateRequest request) {

        validateExistProduct(productId);

        validateDuplicateReview(productId, userId);

        String imageUrl = Optional.ofNullable(request.image())
                .filter(file -> !file.isEmpty())
                .map(imageUploader::uploadProductReviewImage)
                .orElse(null);

        ProductReview productReview = ProductReview.create(productId, userId, request.rating(), request.comment(), imageUrl);

        ProductReview domain = productReviewRepository.save(productReview);

        return ProductReviewResponse.from(domain);
    }

    @Override
    @Transactional(readOnly = true)
    public ScrollResponse<ProductReviewResponse> getProductReviews(Long productId, Long cursorId, int size) {

        List<ProductReviewResponse> reviews = productReviewRepository.findNextReviews(productId, cursorId, size + 1);

        boolean hasNext = reviews.size() > size;
        if (hasNext) {
            reviews = reviews.subList(0, size);
        }

        Long nextCursorId = hasNext ? reviews.get(reviews.size() - 1).reviewId() : null;

        return ScrollResponse.of(reviews, nextCursorId, hasNext);
    }

    @Override
    @Transactional(readOnly = true)
    public ScrollResponse<ProductReviewResponse> getUserReviews(Long userId, Long cursorId, int limit) {
        List<ProductReviewResponse> reviews = productReviewRepository.findNextUserReviews(userId, cursorId, limit + 1);

        boolean hasNext = reviews.size() > limit;
        if (hasNext) {
            reviews = reviews.subList(0, limit);
        }

        Long nextCursorId = hasNext ? reviews.get(reviews.size() - 1).reviewId() : null;

        return ScrollResponse.of(reviews, nextCursorId, hasNext);
    }

    @Override
    @Transactional
    public ProductReviewResponse updateReview(Long reviewId, Long userId, ProductReviewUpdateRequest request) {
        ProductReview productReview = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_REVIEW_NOT_FOUND_ERROR));

        if (!productReview.userId().equals(userId)) {
            throw new ProductException(ErrorCode.PRODUCT_REVIEW_FORBIDDEN);
        }

        String newImageUrl = productReview.imageUrl();

        // 새로운 이미지가 존재할 때
        if (request.image() != null && !request.image().isEmpty()) {
            // 기존 이미지를 제거합니다
            if (productReview.imageUrl() != null) {
                imageUploader.deleteProductReviewImage(productReview.imageUrl());
            }

            newImageUrl = imageUploader.uploadProductReviewImage(request.image());
        }

        ProductReview domain = productReview.update(request.rating(), request.comment(), newImageUrl);

        return ProductReviewResponse.from(productReviewRepository.update(domain));

    }

    @Override
    @Transactional(readOnly = true)
    public ProductReviewStatisticsResponse getReviewStatistics(Long productId) {
        return productReviewRepository.getReviewStatistics(productId);
    }

    // 제품이 등록되어 있는지 검증
    private void validateExistProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR);
        }
    }

    // 사용자가 제품에 대한 리뷰를 이미 등록한 적 있는지 검증
    private void validateDuplicateReview(Long productId, Long userId) {
        if (productReviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new ProductException(ErrorCode.ALREADY_REVIEW_EXISTS);
        }
    }
}
