package com.homesweet.homesweetback.domain.product.cart.repository.mapper;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.product.cart.domain.Cart;
import com.homesweet.homesweetback.domain.product.cart.repository.jpa.entity.CartEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 장바구니 도메인 <-> 엔티티 매핑
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
@Component
public class CartMapper {

    // Entity -> Domain
    public Cart toDomain(CartEntity entity) {

        return Cart.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .skuId(entity.getSku().getId())
                .quantity(entity.getQuantity())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

    }

    // Domain -> Entity
    public CartEntity toEntity(Cart domain) {

        return CartEntity.builder()
                .user(User.builder().id(domain.userId()).build())
                .sku(SkuEntity.builder().id(domain.skuId()).build())
                .quantity(domain.quantity())
                .build();
    }
}
