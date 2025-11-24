package com.homesweet.homesweetback.domain.product.cart.repository.jpa.querydsl;

import com.homesweet.homesweetback.domain.product.cart.controller.response.CartResponse;
import com.homesweet.homesweetback.domain.product.cart.repository.jpa.entity.QCartEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.*;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class CustomCartRepositoryImpl implements CustomCartRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CartResponse> findNextCartItems(Long memberId, Long cursorId, int size) {
        List<Tuple> tuples = queryCartTuples(memberId, cursorId, size);
        if (tuples.isEmpty()) return Collections.emptyList();
        return mapTuplesToCartResponses(tuples);
    }

    /**
     * 카트 목록 및 옵션 정보 쿼리
     */
    private List<Tuple> queryCartTuples(Long memberId, Long cursorId, int size) {
        QCartEntity cart = QCartEntity.cartEntity;
        QSkuEntity sku = QSkuEntity.skuEntity;
        QProductEntity product = QProductEntity.productEntity;
        QProductSkuOptionEntity skuOption = QProductSkuOptionEntity.productSkuOptionEntity;
        QProductOptionValueEntity optionValue = QProductOptionValueEntity.productOptionValueEntity;
        QProductOptionGroupEntity optionGroup = QProductOptionGroupEntity.productOptionGroupEntity;

        return queryFactory
                .select(
                        cart.id,
                        sku.id,
                        sku.product.id,
                        product.brand,
                        product.name,
                        optionGroup.groupName,
                        optionValue.value,
                        product.basePrice,
                        sku.priceAdjustment,
                        product.discountRate,
                        product.shippingPrice,
                        cart.quantity,
                        product.imageUrl,
                        cart.createdAt,
                        cart.updatedAt
                )
                .from(cart)
                .leftJoin(sku).on(cart.sku.eq(sku))
                .leftJoin(product).on(sku.product.eq(product))
                .leftJoin(skuOption).on(skuOption.sku.eq(sku))
                .leftJoin(optionValue).on(optionValue.eq(skuOption.optionValue))
                .leftJoin(optionGroup).on(optionGroup.eq(optionValue.group))
                .where(cart.id.in(
                        JPAExpressions
                                .select(cart.id)
                                .from(cart)
                                .where(buildCartCursorCondition(cart, memberId, cursorId))
                                .orderBy(cart.id.asc())
                                .limit(size + 1)
                ))
                .orderBy(cart.id.asc())
                .fetch();
    }

    /**
     * Tuple → DTO 변환 (옵션 병합 포함)
     */
    private List<CartResponse> mapTuplesToCartResponses(List<Tuple> tuples) {
        QCartEntity cart = QCartEntity.cartEntity;
        QSkuEntity sku = QSkuEntity.skuEntity;
        QProductEntity product = QProductEntity.productEntity;
        QProductOptionGroupEntity optionGroup = QProductOptionGroupEntity.productOptionGroupEntity;
        QProductOptionValueEntity optionValue = QProductOptionValueEntity.productOptionValueEntity;

        Map<Long, CartResponse.CartResponseBuilder> grouped = new LinkedHashMap<>();

        for (Tuple t : tuples) {
            Long cartId = t.get(cart.id);

            grouped.computeIfAbsent(cartId, id -> {
                Integer basePrice = t.get(product.basePrice);
                Integer priceAdjustment = Optional.ofNullable(t.get(sku.priceAdjustment)).orElse(0);
                BigDecimal discountRate = t.get(product.discountRate);
                Integer quantity = t.get(cart.quantity);
                int totalPrice = calculateTotalPrice(basePrice, priceAdjustment, discountRate, quantity);

                return CartResponse.builder()
                        .id(id)
                        .skuId(t.get(sku.id))
                        .productId(t.get(sku.product.id))
                        .brand(t.get(product.brand))
                        .productName(t.get(product.name))
                        .basePrice(basePrice)
                        .discountRate(discountRate)
                        .finalPrice((int) Math.floor(basePrice * (1 - discountRate.doubleValue() / 100)) + priceAdjustment)
                        .shippingPrice(t.get(product.shippingPrice))
                        .quantity(quantity)
                        .totalPrice(totalPrice)
                        .imageUrl(t.get(product.imageUrl))
                        .createdAt(t.get(cart.createdAt))
                        .updatedAt(t.get(cart.updatedAt))
                        .priceAdjustment(priceAdjustment)
                        .optionSummary("");
            });

            String groupName = t.get(optionGroup.groupName);
            String value = t.get(optionValue.value);
            if (groupName != null && value != null) {
                CartResponse.CartResponseBuilder builder = grouped.get(cartId);
                String existing = builder.build().optionSummary();
                String newOption = groupName + ": " + value;
                builder.optionSummary(existing.isEmpty() ? newOption : existing + " / " + newOption);
            }
        }

        return grouped.values().stream()
                .map(CartResponse.CartResponseBuilder::build)
                .toList();
    }

    /**
     * 할인 및 총합 계산
     */
    private int calculateTotalPrice(int basePrice, int adjustment, BigDecimal discountRate, int quantity) {
        double discountedBase = basePrice * (1 - discountRate.doubleValue() / 100);
        int finalPrice = (int) Math.floor(discountedBase) + adjustment;
        return finalPrice * quantity;
    }

    /**
     * 커서 조건
     */
    private BooleanExpression buildCartCursorCondition(QCartEntity cart, Long memberId, Long cursorId) {
        BooleanExpression condition = cart.user.id.eq(memberId);
        if (cursorId != null) {
            condition = condition.and(cart.id.gt(cursorId));
        }
        return condition;
    }
}