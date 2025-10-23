package com.homesweet.homesweetback.domain.product.review.service.impl;

import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.repository.ProductRepository;
import com.homesweet.homesweetback.domain.product.product.repository.util.ProductImageUploader;
import com.homesweet.homesweetback.domain.product.review.controller.ProductReviewController;
import com.homesweet.homesweetback.domain.product.review.controller.request.ProductReviewCreateRequest;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewResponse;
import com.homesweet.homesweetback.domain.product.review.domain.ProductReview;
import com.homesweet.homesweetback.domain.product.review.repository.ProductReviewRepository;
import com.homesweet.homesweetback.domain.product.review.service.ProductReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        ProductReview review = ProductReview.createReview(

        )

        return null;
    }

    private void validateExistProduct(Long productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ProductException("존재하지 않는 상품입니다."));
    }

    private void validateDuplicateReview(Long productId, Long userId) {
        productReviewRepository.findByProductAndUser(product, user)
                .orElseThrow(() -> new ProductException(~~));
    }
}
