package com.homesweet.homesweetback.domain.product.cart.controller.request;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
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

    public void validateLimitQuantity() {
        // 수량 제한 체크
        if (this.quantity() > 10) {
            throw new ProductException(ErrorCode.CART_LIMIT_EXCEEDED_ERROR);
        }
    }
}
