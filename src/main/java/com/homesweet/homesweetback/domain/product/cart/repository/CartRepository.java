package com.homesweet.homesweetback.domain.product.cart.repository;

import com.homesweet.homesweetback.domain.product.cart.domain.Cart;

import java.util.Optional;

/**
 * 장바구니 레포 명세
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
public interface CartRepository {

    Optional<Cart> findByUserIdAndSkuId(Long userId, Long skuId);

    Cart save(Cart cart);

    Cart updateQuantity(Cart domain);
}
