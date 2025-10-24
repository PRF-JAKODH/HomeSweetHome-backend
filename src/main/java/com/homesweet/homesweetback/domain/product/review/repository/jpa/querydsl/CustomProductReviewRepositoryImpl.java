package com.homesweet.homesweetback.domain.product.review.repository.jpa.querydsl;

import com.homesweet.homesweetback.domain.auth.entity.QUser;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.QProductEntity;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewResponse;
import com.homesweet.homesweetback.domain.product.review.repository.jpa.entity.QProductReviewEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 상품 리뷰 QueryDSL 구현체
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
@Repository
@RequiredArgsConstructor
public class CustomProductReviewRepositoryImpl implements CustomProductReviewRepository{

    private final JPAQueryFactory queryFactory;
    QProductReviewEntity review = QProductReviewEntity.productReviewEntity;
    QProductEntity product = QProductEntity.productEntity;

    @Override
    public List<ProductReviewResponse> findNextReviews(Long productId, Long cursorId, int size) {
        BooleanExpression condition = buildCursorCondition(review, productId, cursorId);

        return queryFactory
                .select(Projections.constructor(ProductReviewResponse.class,
                        review.id,
                        review.product.id,
                        review.user.id,
                        review.rating,
                        review.comment,
                        review.imageUrl,
                        review.createdAt,
                        review.updatedAt
                ))
                .from(review)
                .where(condition)
                .orderBy(review.id.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public List<ProductReviewResponse> findNextUserReviews(Long userId, Long cursorId, int limit) {
        BooleanExpression condition = review.user.id.eq(userId);
        if (cursorId != null) {
            condition = condition.and(review.id.lt(cursorId)); // 최신순
        }

        return queryFactory
                .select(Projections.constructor(ProductReviewResponse.class,
                        review.id,
                        review.product.id,
                        review.user.id,
                        review.rating,
                        review.comment,
                        review.imageUrl,
                        review.createdAt,
                        review.updatedAt
                ))
                .from(review)
                .join(review.product, product)
                .where(condition)
                .orderBy(review.id.desc())
                .limit(limit)
                .fetch();
    }

    // 최신순 기준 → cursorId보다 작은 id만 조회
    private BooleanExpression buildCursorCondition(QProductReviewEntity review, Long productId, Long cursorId) {
        BooleanExpression condition = review.product.id.eq(productId);

        if (cursorId != null) {
            condition = condition.and(review.id.lt(cursorId));
        }

        return condition;
    }
}
