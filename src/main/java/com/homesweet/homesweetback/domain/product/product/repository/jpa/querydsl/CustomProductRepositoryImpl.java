package com.homesweet.homesweetback.domain.product.product.repository.jpa.querydsl;

import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.QProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.category.service.cache.CacheCategory;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.request.search.ProductFilterRequest;
import com.homesweet.homesweetback.domain.product.product.controller.response.*;
import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.*;
import com.homesweet.homesweetback.domain.product.review.repository.jpa.entity.QProductReviewEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.QProductCategoryEntity.*;
import static com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.QProductEntity.*;
import static com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.QProductOptionGroupEntity.*;
import static com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.QProductOptionValueEntity.*;
import static com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.QProductSkuOptionEntity.*;
import static com.homesweet.homesweetback.domain.product.review.repository.jpa.entity.QProductReviewEntity.productReviewEntity;

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
        StringBuilder sql = new StringBuilder();
        sql.append("""
        SELECT
            p.product_id,
            p.category_id,
            p.user_id AS seller_id,
            p.name,
            p.image_url,
            p.brand,
            p.base_price,
            p.discount_rate,
            p.shipping_price,
            p.status,

            (
                SELECT COALESCE(AVG(r.rating), 0)
                FROM products_reviews r
                WHERE r.product_id = p.product_id
            ) AS average_rating,

            (
                SELECT COALESCE(COUNT(r2.review_id), 0)
                FROM products_reviews r2
                WHERE r2.product_id = p.product_id
            ) AS review_count,

            p.created_at,
            p.updated_at

        FROM products p
        WHERE 1 = 1
        """);

        // 1) 키워드(fulltext) 조건
        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                AND MATCH(p.name, p.brand, p.description)
                    AGAINST(:keyword IN BOOLEAN MODE)
            """);
        }

        // 2) 카테고리(서브카테고리 포함) 조건
        List<Long> categoryIds = cacheCategory.getAllSubCategoryIds(categoryId);

        if (categoryIds != null && !categoryIds.isEmpty()) {
            String inClause = categoryIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

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
    public List<ProductPreviewResponse> findProductsByOptionFilter(
            Long cursorId,
            ProductFilterRequest request,
            int limit,
            ProductSortType sortType
    ) {
        QProductEntity product = productEntity;
        QProductReviewEntity review = productReviewEntity;

        List<Long> allSubCategoryIds = cacheCategory.getAllSubCategoryIds(request.categoryId());

        BooleanExpression keywordCondition = buildKeywordCondition(product, request.keyword());
        BooleanExpression cursorCondition = buildCursorCondition(product, cursorId, sortType);
        BooleanExpression categoryCondition = buildCategoryCondition(product, allSubCategoryIds);
        BooleanExpression statusCondition = buildStatusCondition(product);
        BooleanExpression optionCondition = buildOptionFilterCondition(product, request);
        BooleanExpression rangeFilterCondition = buildRangeFilterCondition(product, request);

        BooleanBuilder builder = new BooleanBuilder()
                .and(keywordCondition)
                .and(cursorCondition)
                .and(categoryCondition)
                .and(statusCondition)
                .and(optionCondition)
                .and(rangeFilterCondition);

        OrderSpecifier<?> orderSpecifier = buildOrderSpecifier(product, sortType);

        List<ProductPreviewResponse> results = queryFactory
                .select(Projections.constructor(ProductPreviewResponse.class,
                        product.id,
                        product.category.id,
                        product.seller.id,
                        product.name,
                        product.imageUrl,
                        product.brand,
                        product.basePrice,
                        product.discountRate,
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
                .where(builder)
                .orderBy(orderSpecifier)
                .limit(limit + 1)
                .fetch();

        return results;
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
                .leftJoin(skuOption).on(skuOption.sku.eq(sku))
                .leftJoin(optionValue).on(optionValue.eq(skuOption.optionValue))
                .leftJoin(optionGroup).on(optionGroup.eq(optionValue.group))
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
    public Optional<ProductDetailResponse> findProductDetailById(Long productId) {
        QProductEntity product = QProductEntity.productEntity;
        QProductDetailImageEntity detailImage = QProductDetailImageEntity.productDetailImageEntity;

        ProductEntity entity = queryFactory
                .selectFrom(product)
                .leftJoin(product.category).fetchJoin()
                .leftJoin(product.seller).fetchJoin()
                .where(product.id.eq(productId))
                .fetchOne();

        if (entity == null) {
            return Optional.empty();
        }

        List<String> detailImageUrls = queryFactory
                .select(detailImage.imageUrl)
                .from(detailImage)
                .where(detailImage.product.id.eq(productId))
                .orderBy(detailImage.id.asc())
                .fetch();

        return Optional.of(ProductDetailResponse.from(entity, detailImageUrls));
    }

    @Override
    public List<ProductManageResponse> findProductsForSeller(Long sellerId, String startDate, String endDate) {

        QProductEntity product = QProductEntity.productEntity;
        QSkuEntity sku = QSkuEntity.skuEntity;
        QProductCategoryEntity category = productCategoryEntity;
        QProductCategoryEntity parent = new QProductCategoryEntity("parent");
        QProductCategoryEntity grandParent = new QProductCategoryEntity("grandParent");

        BooleanExpression condition = product.seller.id.eq(sellerId);

        // 날짜 필터링
        condition = condition
                .and(parseStartDate(startDate))
                .and(parseEndDate(endDate));

        return queryFactory
                .select(Projections.constructor(ProductManageResponse.class,
                        product.id,
                        product.name,
                        product.imageUrl,
                        Expressions.stringTemplate(
                                "concat_ws(' > ', {0}, {1}, {2})",
                                grandParent.name, parent.name, category.name
                        ),
                        product.basePrice,
                        product.discountRate,
                        product.shippingPrice,
                        JPAExpressions
                                .select(sku.stockQuantity.sum().coalesce(0L))
                                .from(sku)
                                .where(sku.product.id.eq(product.id)),
                        product.status,
                        product.createdAt
                ))
                .from(product)
                .join(product.category, category)
                .leftJoin(parent).on(category.parentId.eq(parent.id))
                .leftJoin(grandParent).on(parent.parentId.eq(grandParent.id))
                .where(condition)
                .orderBy(product.createdAt.desc())
                .fetch();
    }

    /**
     * 옵션 필터 조건 생성
     * 같은 그룹 내에서는 OR (색상=빨강 OR 색상=파랑)
     * 다른 그룹 간에는 AND (색상 조건 AND 사이즈 조건)
     */
    private BooleanExpression buildOptionFilterCondition(
            QProductEntity product,
            ProductFilterRequest request
    ) {
        if (!request.hasOptionFilters()) {
            return null;
        }

        QProductOptionGroupEntity optionGroup = productOptionGroupEntity;
        QProductOptionValueEntity optionValue = productOptionValueEntity;

        BooleanExpression finalCondition = null;

        // 각 옵션 그룹별로 처리
        for (Map.Entry<String, List<String>> entry : request.optionFilters().entrySet()) {
            String groupName = entry.getKey();
            List<String> values = entry.getValue();

            if (values == null || values.isEmpty()) {
                continue;
            }

            // 해당 그룹 내에서는 OR 조건
            BooleanExpression groupCondition;

            if (values.size() == 1) {
                // 값이 하나일 때는 eq 사용
                groupCondition = JPAExpressions
                        .selectOne()
                        .from(optionGroup)
                        .join(optionGroup.values, optionValue)
                        .where(
                                optionGroup.product.id.eq(product.id),
                                optionGroup.groupName.containsIgnoreCase(groupName),
                                optionValue.value.eq(values.getFirst())
                        )
                        .exists();
            } else {
                // 값이 여러 개일 때는 in 사용
                groupCondition = JPAExpressions
                        .selectOne()
                        .from(optionGroup)
                        .join(optionGroup.values, optionValue)
                        .where(
                                optionGroup.product.id.eq(product.id),
                                optionGroup.groupName.containsIgnoreCase(groupName),
                                optionValue.value.in(values)
                        )
                        .exists();
            }

            // 그룹 간에는 AND 조건
            finalCondition = (finalCondition == null)
                    ? groupCondition
                    : finalCondition.and(groupCondition);
        }

        return finalCondition;
    }

    /**
     * 범위 필터 조건 생성
     * 옵션 값에서 숫자를 추출하여 범위 비교
     * 예: "가로 길이" 옵션에 "100cm", "100" 등의 값이 있고, 범위가 90-110이면 매칭
     */
    private BooleanExpression buildRangeFilterCondition(
            QProductEntity product,
            ProductFilterRequest request
    ) {
        if (!request.hasRangeFilters()) {
            return null;
        }

        QProductOptionGroupEntity optionGroup = productOptionGroupEntity;
        QProductOptionValueEntity optionValue = productOptionValueEntity;

        BooleanExpression finalCondition = null;

        for (Map.Entry<String, ProductFilterRequest.RangeFilter> entry : request.rangeFilters().entrySet()) {
            String groupName = entry.getKey();
            ProductFilterRequest.RangeFilter range = entry.getValue();

            BooleanExpression groupCondition = JPAExpressions
                    .selectOne()
                    .from(optionGroup)
                    .join(optionGroup.values, optionValue)
                    .where(
                            optionGroup.product.id.eq(product.id),
                            optionGroup.groupName.containsIgnoreCase(groupName),
                            buildNumericRangeCondition(optionValue, range)
                    )
                    .exists();

            finalCondition = (finalCondition == null)
                    ? groupCondition
                    : finalCondition.and(groupCondition);
        }

        return finalCondition;
    }

    /**
     * 옵션 값에서 숫자를 추출하여 범위 비교
     * 예: "100" → 100
     */
    private BooleanExpression buildNumericRangeCondition(
            QProductOptionValueEntity optionValue,
            ProductFilterRequest.RangeFilter range
    ) {
        NumberExpression<Integer> numericValue =
                optionValue.value.castToNum(Integer.class);

        BooleanExpression condition = null;

        if (range.hasMin()) {
            condition = numericValue.goe(range.minValue());
        }

        if (range.hasMax()) {
            BooleanExpression maxCondition = numericValue.loe(range.maxValue());
            condition = (condition == null) ? maxCondition : condition.and(maxCondition);
        }

        return condition;
    }

    // 판매자 상품 조회 시작일
    private BooleanExpression parseStartDate(String startDate) {
        if (startDate == null || startDate.isEmpty()) return null;
        LocalDateTime start = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE).atStartOfDay();
        return QProductEntity.productEntity.createdAt.goe(start);
    }

    // 판매자 상품 조회 끝일
    private BooleanExpression parseEndDate(String endDate) {
        if (endDate == null || endDate.isEmpty()) return null;
        LocalDateTime end = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE).atTime(LocalTime.MAX);
        return QProductEntity.productEntity.createdAt.loe(end);
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
            case POPULAR -> Expressions.numberTemplate(Long.class,
                            "(select count(r) from ProductReviewEntity r where r.product.id = {0})",
                            product.id)
                    .desc();
            default -> product.createdAt.desc();
        };
    }
}
