package com.homesweet.homesweetback.domain.product.product.repository.jpa.querydsl;

import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductPreviewResponse;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.QProductEntity;
import com.homesweet.homesweetback.domain.product.review.repository.jpa.entity.QProductReviewEntity;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 제품 QueryDSL 레포 구현체
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
@Repository
@RequiredArgsConstructor
public class CustomProductRepositoryImpl implements CustomProductRepository{

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ProductPreviewResponse> findNextProducts(Long cursorId, int size, String keyword, ProductSortType sortType) {
        QProductEntity product = QProductEntity.productEntity;
        QProductReviewEntity review = QProductReviewEntity.productReviewEntity;

        BooleanExpression condition = Expressions.allOf(
                buildKeywordCondition(product, keyword),
                buildCursorCondition(product, cursorId, sortType)
        );

        OrderSpecifier<?> orderSpecifier = buildOrderSpecifier(product, sortType);

        return queryFactory
                .select(Projections.constructor(ProductPreviewResponse.class,
                        product.id,
                        product.category.id,
                        product.name,
                        product.imageUrl,
                        product.brand,
                        product.basePrice,
                        product.discountRate,
                        product.description,
                        product.shippingPrice,
                        product.status,
                        JPAExpressions
                                .select(review.rating.avg())
                                .from(review)
                                .where(review.product.id.eq(product.id)),
                        JPAExpressions
                                .select(review.count())
                                .from(review)
                                .where(review.product.id.eq(product.id)),
                        product.createdAt,
                        product.updatedAt
                ))
                .from(product)
                .where(condition)
                .orderBy(orderSpecifier)
                .limit(size + 1)
                .fetch();
    }

    // 검색 조건 (제품명 or 브랜드)
    private BooleanExpression buildKeywordCondition(QProductEntity product, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return product.name.containsIgnoreCase(keyword)
                .or(product.brand.containsIgnoreCase(keyword));
    }

    // 커서 조건 (정렬 방향)
    private BooleanExpression buildCursorCondition(QProductEntity product, Long cursorId, ProductSortType sortType) {
        if (cursorId == null) return null;

        return switch (sortType) {
            case PRICE_LOW, LATEST, POPULAR -> product.id.lt(cursorId);
            case PRICE_HIGH -> product.id.gt(cursorId);
        };
    }

    // 정렬 조건 생성
    private OrderSpecifier<?> buildOrderSpecifier(QProductEntity product, ProductSortType sortType) {
        return switch (sortType) {
            case LATEST -> product.createdAt.desc();
            case PRICE_HIGH -> product.basePrice.desc();
            case PRICE_LOW -> product.basePrice.asc();
            case POPULAR -> product.basePrice.desc();  // TODO: 리뷰 많은 순
            default -> product.createdAt.desc();
        };
    }
}
