package com.homesweet.homesweetback.domain.product.product.controller.request.update;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 상품 재고 업데이트 요청 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 30.
 */
public record ProductSkuUpdateRequest(

        @NotNull(message = "재고 정보 리스트는 필수입니다.")
        List<SkuStockUpdateRequest> skus
) {

    /**
     * 개별 SKU 재고 업데이트 요청
     */
    public record SkuStockUpdateRequest(
            @NotNull(message = "SKU ID는 필수입니다.")
            Long skuId,

            @NotNull(message = "재고 수량은 필수입니다.")
            @Min(value = 0, message = "재고는 0개 이상이어야 합니다.")
            Long stockQuantity,

            Integer priceAdjustment
    ) {}
}
