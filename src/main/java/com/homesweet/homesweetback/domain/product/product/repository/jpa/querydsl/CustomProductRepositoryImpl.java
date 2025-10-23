package com.homesweet.homesweetback.domain.product.product.repository.jpa.querydsl;

import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductPreviewResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.SkuStockResponse;
import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.*;
import com.homesweet.homesweetback.domain.product.review.repository.jpa.entity.QProductReviewEntity;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.QProductEntity.*;
import static com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.QProductOptionGroupEntity.*;
import static com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.QProductOptionValueEntity.*;
import static com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.QProductSkuOptionEntity.*;
import static com.homesweet.homesweetback.domain.product.review.repository.jpa.entity.QProductReviewEntity.*;

/**
 * 제품 QueryDSL 레포 구현체
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomProductRepositoryImpl implements CustomProductRepository{

    private final JPAQueryFactory queryFactory;
    private final ProductCategoryRepository categoryRepository;

    @Override
    public List<ProductPreviewResponse> findNextProducts(Long cursorId, Long categoryId, int limit, String keyword, ProductSortType sortType) {
        QProductEntity product = productEntity;
        QProductReviewEntity review = productReviewEntity;

        List<Long> allSubCategoryIds = categoryRepository.findAllSubCategoryIds(categoryId);

        BooleanExpression condition = Expressions.allOf(
                buildKeywordCondition(product, keyword),
                buildCursorCondition(product, cursorId, sortType),
                buildCategoryCondition(product, allSubCategoryIds),
                buildStatusCondition(product)
        );

        OrderSpecifier<?> orderSpecifier = buildOrderSpecifier(product, sortType);

        return queryFactory
                .select(Projections.constructor(ProductPreviewResponse.class,
                        product.id,
                        product.category.id,
                        product.seller.id,
                        product.name,
                        product.imageUrl,
                        product.brand,
                        product.basePrice,
                        product.discountRate,
                        product.description,
                        product.shippingPrice,
                        product.status,
                        JPAExpressions
                                .select(review.rating.avg().coalesce(0.0))
                                .from(review)
                                .where(review.product.id.eq(product.id)),
                        JPAExpressions
                                .select(review.count().coalesce(0L))
                                .from(review)
                                .where(review.product.id.eq(product.id)),
                        product.createdAt,
                        product.updatedAt
                ))
                .from(product)
                .where(condition)
                .orderBy(orderSpecifier)
                .limit(limit + 1)
                .fetch();
    }

    @Override
    public List<SkuStockResponse> findSkuStocksByProductId(Long productId) {
        QSkuEntity sku = QSkuEntity.skuEntity;
        QProductSkuOptionEntity skuOption = productSkuOptionEntity;
        QProductOptionValueEntity optionValue = productOptionValueEntity;
        QProductOptionGroupEntity optionGroup = productOptionGroupEntity;

        // SKU 별 옵션 조합 조회
        List<Tuple> tuples = queryFactory
                .select(
                        sku.id,
                        sku.stockQuantity,
                        sku.priceAdjustment,
                        optionGroup.groupName,
                        optionValue.value
                )
                .from(sku)
                .join(skuOption).on(skuOption.sku.eq(sku))
                .join(optionValue).on(optionValue.eq(skuOption.optionValue))
                .join(optionGroup).on(optionGroup.eq(optionValue.group))
                .where(sku.product.id.eq(productId))
                .orderBy(sku.id.asc())
                .fetch();

        // SKU별로 옵션 조합을 묶어서 반환
        Map<Long, SkuStockResponse> skuMap = new LinkedHashMap<>();

        for (Tuple t : tuples) {
            Long skuId = t.get(sku.id);
            skuMap.computeIfAbsent(skuId, id ->
                    new SkuStockResponse(
                            id,
                            t.get(sku.stockQuantity),
                            t.get(sku.priceAdjustment),
                            new ArrayList<>()
                    )
            );

            skuMap.get(skuId).options()
                    .add(new SkuStockResponse.OptionCombinationResponse(
                            t.get(optionGroup.groupName),
                            t.get(optionValue.value)
                    ));
        }

        return new ArrayList<>(skuMap.values());
    }

    @Override
    public ProductPreviewResponse findProductDetailById(Long productId) {
        QProductEntity product = productEntity;
        QProductReviewEntity review = productReviewEntity;

        return queryFactory
                .select(Projections.constructor(ProductPreviewResponse.class,
                        product.id,
                        product.category.id,
                        product.seller.id,
                        product.name,
                        product.imageUrl,
                        product.brand,
                        product.basePrice,
                        product.discountRate,
                        product.description,
                        product.shippingPrice,
                        product.status,
                        JPAExpressions
                                .select(review.rating.avg().coalesce(0.0))
                                .from(review)
                                .where(review.product.id.eq(product.id)),
                        JPAExpressions
                                .select(review.count().coalesce(0L))
                                .from(review)
                                .where(review.product.id.eq(product.id)),
                        product.createdAt,
                        product.updatedAt
                ))
                .from(product)
                .where(product.id.eq(productId))
                .fetchOne();
    }

    // 카테고리를 선택하면 하위 카테고리에 해당하는 모든 상품이 조회되어야 한다
    private BooleanExpression buildCategoryCondition(QProductEntity product, List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return null;
        }
        return product.category.id.in(categoryIds);
    }

    // 판매 중지 상품은 조회되면 안 된다
    private BooleanExpression buildStatusCondition(QProductEntity product) {
        return product.status.ne(ProductStatus.SUSPENDED);
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
