package com.homesweet.homesweetback.domain.product.review.repository.jpa.querydsl;

import com.homesweet.homesweetback.domain.auth.entity.QUser;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.QProductEntity;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewResponse;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewStatisticsResponse;
import com.homesweet.homesweetback.domain.product.review.repository.jpa.entity.QProductReviewEntity;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    QUser user = QUser.user;

    @Override
    public List<ProductReviewResponse> findNextReviews(Long productId, Long cursorId, int size) {
        BooleanExpression condition = buildCursorCondition(review, productId, cursorId);

        return queryFactory
                .select(Projections.constructor(ProductReviewResponse.class,
                        review.id,
                        review.product.id,
                        review.user.id,
                        review.product.name,
                        user.name,
                        review.rating,
                        review.comment,
                        review.imageUrl,
                        review.createdAt,
                        review.updatedAt
                ))
                .from(review)
                .join(review.user, user)
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
                        review.product.name,
                        user.name,
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

    @Override
    public ProductReviewStatisticsResponse getReviewStatistics(Long productId) {
        // 전체 리뷰 수 + 평균 평점
        Tuple overall = queryFactory
                .select(
                        review.count(),
                        review.rating.avg()
                )
                .from(review)
                .where(review.product.id.eq(productId))
                .fetchOne();

        long totalCount = overall != null && overall.get(review.count()) != null
                ? overall.get(review.count())
                : 0L;
        double averageRating = overall != null && overall.get(review.rating.avg()) != null
                ? Math.round(overall.get(review.rating.avg()) * 10.0) / 10.0
                : 0.0;

        // 각 별점별 카운트
        List<Tuple> counts = queryFactory
                .select(review.rating, review.count())
                .from(review)
                .where(review.product.id.eq(productId))
                .groupBy(review.rating)
                .fetch();

        Map<Integer, Long> ratingCounts = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingCounts.put(i, 0L);
        }
        for (Tuple t : counts) {
            ratingCounts.put(
                    t.get(review.rating).intValue(),
                    t.get(review.count())
            );
        }

        return ProductReviewStatisticsResponse.of(productId, totalCount, averageRating, ratingCounts);
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
