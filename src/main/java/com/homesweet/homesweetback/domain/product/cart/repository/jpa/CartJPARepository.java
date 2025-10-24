package com.homesweet.homesweetback.domain.product.cart.repository.jpa;

import com.homesweet.homesweetback.domain.product.cart.repository.jpa.entity.CartEntity;
import com.homesweet.homesweetback.domain.product.cart.repository.jpa.querydsl.CustomCartRepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 장바구니 JPA 레포
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
public interface CartJPARepository extends JpaRepository<CartEntity, Long>, CustomCartRepository {
    List<CartEntity> sku(SkuEntity sku);

    Optional<CartEntity> findByUserIdAndSkuId(Long userId, Long skuId);

    boolean existsByIdAndUserId(Long cartId, Long userId);

    void deleteById(Long cartId);
}
