package com.homesweet.homesweetback.domain.product.cart.repository.jpa.querydsl;

import com.homesweet.homesweetback.domain.product.cart.controller.response.CartResponse;

import java.util.List;

/**
 * 장바구니 queryDsl 인터페이스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
public interface CustomCartRepository {

    List<CartResponse> findNextCartItems(Long memberId, Long cursorId, int size);

}
