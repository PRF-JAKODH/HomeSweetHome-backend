package com.homesweet.homesweetback.domain.product.cart.controller.request;

import java.util.List;

/**
 * 장바구니 선택된 제품 모두 제거 요청 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 26.
 */
public record DeleteCartItemsRequest(
        List<Long> cartIds
) {
}
