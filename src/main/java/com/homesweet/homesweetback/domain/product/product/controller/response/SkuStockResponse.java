package com.homesweet.homesweetback.domain.product.product.controller.response;

import java.util.List;

/**
 * 상품 -> 옵션 조합 별 재고 및 추가 금액 응답 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
public record SkuStockResponse(
        Long skuId,
        Integer stockQuantity,
        Integer priceAdjustment,
        List<OptionCombinationResponse> options
) {

    public record OptionCombinationResponse(
            String groupName,
            String valueName
    ) {}

}
