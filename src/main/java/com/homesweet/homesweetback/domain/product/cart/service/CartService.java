package com.homesweet.homesweetback.domain.product.cart.service;

import com.homesweet.homesweetback.common.util.ScrollResponse;
import com.homesweet.homesweetback.domain.product.cart.controller.request.CartRequest;
import com.homesweet.homesweetback.domain.product.cart.controller.response.CartResponse;
import com.homesweet.homesweetback.domain.product.cart.domain.Cart;

import java.util.List;

/**
 * 장바구니 서비스 명세
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
public interface CartService {

    Cart addToCart(Long userId, CartRequest request);

    ScrollResponse<CartResponse> getCartItems(Long userId, Long cursorId, int size);

    void deleteCartItem(Long userId, Long cartId);

    void deleteSelectedCartItems(Long userId, List<Long> cartIds);
}
