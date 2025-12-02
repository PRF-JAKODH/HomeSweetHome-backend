package com.homesweet.homesweetback.domain.product.cart.repository;

import com.homesweet.homesweetback.domain.product.cart.controller.response.CartResponse;
import com.homesweet.homesweetback.domain.product.cart.domain.Cart;

import java.util.List;
import java.util.Optional;

/**
 * 장바구니 레포 명세s
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
public interface CartRepository {

    Cart save(Cart cart);

    Optional<Cart> findByUserIdAndSkuId(Long userId, Long skuId);

    Cart updateQuantity(Cart domain);

    List<CartResponse> findNextCartItems(Long memberId, Long cursorId, int size);

    Optional<Cart> findById(Long cartId);

    boolean existsByIdAndUserId(Long cartId, Long userId);

    void deleteById(Long cartId);

    void deleteAllByUserIdAndCartIdIn(Long userId, List<Long> cartIds);

    int countByUserId(Long userId);

    void deleteByUserIdAndSkuIdIn(Long userId, List<Long> skuIds); // 장바구니 구매 완료 상품 삭제 - 안채호

    void deleteAll(); //테스트 드래곤
}
