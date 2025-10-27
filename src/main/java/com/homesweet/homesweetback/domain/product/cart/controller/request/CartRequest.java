package com.homesweet.homesweetback.domain.product.cart.controller.request;

import lombok.Builder;

/**
 * 장바구니 생성 요청 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
@Builder
public record CartRequest(
        Long skuId,
        Integer quantity
) {
}
