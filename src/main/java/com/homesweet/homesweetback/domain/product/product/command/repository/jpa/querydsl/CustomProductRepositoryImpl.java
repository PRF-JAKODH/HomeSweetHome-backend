package com.homesweet.homesweetback.domain.product.product.command.repository.jpa.querydsl;

import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.QProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.*;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.ProductDetailResponse;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.ProductManageResponse;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.SkuStockResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.QProductCategoryEntity.*;
import static com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.QProductOptionGroupEntity.productOptionGroupEntity;
import static com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.QProductOptionValueEntity.productOptionValueEntity;
import static com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.QProductSkuOptionEntity.productSkuOptionEntity;

/**
 * 제품 QueryDSL 레포 구현체
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomProductRepositoryImpl implements CustomProductRepository {

    private final JPAQueryFactory queryFactory;

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
}
