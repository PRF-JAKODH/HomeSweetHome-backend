package com.homesweet.homesweetback.domain.product.cart.repository.jpa.querydsl;

import com.homesweet.homesweetback.domain.product.cart.controller.response.CartResponse;
import com.homesweet.homesweetback.domain.product.cart.repository.jpa.entity.QCartEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.*;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
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
        QCartEntity cart = QCartEntity.cartEntity;
        QSkuEntity sku = QSkuEntity.skuEntity;
        QProductEntity product = QProductEntity.productEntity;
        QProductSkuOptionEntity skuOption = QProductSkuOptionEntity.productSkuOptionEntity;
        QProductOptionValueEntity optionValue = QProductOptionValueEntity.productOptionValueEntity;
        QProductOptionGroupEntity optionGroup = QProductOptionGroupEntity.productOptionGroupEntity;

        // 먼저 cart_id만 가져오기 (limit 적용 시 중복 제거)
        List<Long> cartIds = queryFactory
                .select(cart.id)
                .from(cart)
                .where(buildCartCursorCondition(cart, memberId, cursorId))
                .orderBy(cart.id.desc())
                .limit(size + 1)
                .fetch();

        if (cartIds.isEmpty()) return Collections.emptyList();

        // 실제 cart 데이터 조회 (옵션 join 포함)
        List<Tuple> tuples = queryFactory
                .select(
                        cart.id,
                        sku.id,
                        product.brand,
                        product.name,
                        optionGroup.groupName,
                        optionValue.value,
                        product.basePrice,
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
                .where(cart.id.in(cartIds))
                .orderBy(cart.id.desc())
                .fetch();

        // 카트별로 옵션 병합
        Map<Long, CartResponse.CartResponseBuilder> grouped = new LinkedHashMap<>();

        for (Tuple t : tuples) {
            Long cartId = t.get(cart.id);
            Integer basePrice = t.get(product.basePrice);
            BigDecimal discountRate = t.get(product.discountRate);
            Integer quantity = t.get(cart.quantity);

            int finalPrice = (int) Math.floor(basePrice * (1 - discountRate.doubleValue() / 100));
            int totalPrice = finalPrice * quantity;

            grouped.computeIfAbsent(cartId, id -> CartResponse.builder()
                    .id(id)
                    .skuId(t.get(sku.id))
                    .brand(t.get(product.brand))
                    .productName(t.get(product.name))
                    .basePrice(basePrice)
                    .discountRate(discountRate)
                    .finalPrice(finalPrice)
                    .shippingPrice(t.get(product.shippingPrice))
                    .quantity(quantity)
                    .totalPrice(totalPrice)
                    .imageUrl(t.get(product.imageUrl))
                    .createdAt(t.get(cart.createdAt))
                    .updatedAt(t.get(cart.updatedAt))
                    .optionSummary("")
            );

            // 옵션 정보 병합
            String groupName = t.get(optionGroup.groupName);
            String value = t.get(optionValue.value);
            if (groupName != null && value != null) {
                CartResponse.CartResponseBuilder builder = grouped.get(cartId);
                String existing = builder.build().optionSummary();
                String newOption = groupName + ": " + value;
                builder.optionSummary(existing.isEmpty() ? newOption : existing + " / " + newOption);
            }
        }

        // DTO 변환
        return grouped.values().stream()
                .map(CartResponse.CartResponseBuilder::build)
                .toList();
    }

    private BooleanExpression buildCartCursorCondition(QCartEntity cart, Long memberId, Long cursorId) {
        BooleanExpression condition = cart.user.id.eq(memberId);
        if (cursorId != null) {
            condition = condition.and(cart.id.lt(cursorId));
        }
        return condition;
    }
}