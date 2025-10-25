package com.homesweet.homesweetback.domain.product.review.repository.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewResponse;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewStatisticsResponse;
import com.homesweet.homesweetback.domain.product.review.domain.ProductReview;
import com.homesweet.homesweetback.domain.product.review.repository.ProductReviewRepository;
import com.homesweet.homesweetback.domain.product.review.repository.jpa.ProductReviewJPARepository;
import com.homesweet.homesweetback.domain.product.review.repository.jpa.entity.ProductReviewEntity;
import com.homesweet.homesweetback.domain.product.review.repository.mapper.ProductReviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 제품 리뷰 레포 구현 코드
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
@Repository
@RequiredArgsConstructor
public class ProductReviewRepositoryImpl implements ProductReviewRepository {

    private final ProductReviewJPARepository jpaRepository;
    private final ProductReviewMapper mapper;

    @Override
    public ProductReview save(ProductReview domain) {
        ProductReviewEntity entity = jpaRepository.save(mapper.toEntity(domain));
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<ProductReview> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByProductIdAndUserId(Long productId, Long userId) {

        return jpaRepository.existsByProductIdAndUserId(productId, userId);
    }

    @Override
    public List<ProductReviewResponse> findNextReviews(Long productId, Long cursorId, int size) {
        return jpaRepository.findNextReviews(productId, cursorId, size);
    }

    @Override
    public List<ProductReviewResponse> findNextUserReviews(Long userId, Long cursorId, int limit) {
        return jpaRepository.findNextUserReviews(userId, cursorId, limit);
    }

    @Override
    public ProductReview update(ProductReview domain) {
        ProductReviewEntity entity = jpaRepository.findById(domain.id())
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_REVIEW_NOT_FOUND_ERROR));

        entity.update(domain.rating(), domain.comment(), domain.imageUrl());

        return mapper.toDomain(entity);
    }

    @Override
    public ProductReviewStatisticsResponse getReviewStatistics(Long productId) {
        return jpaRepository.getReviewStatistics(productId);
    }
}
