package com.homesweet.homesweetback.domain.product.product.controller.request.update;

import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 제품 상태 업데이트 요청 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 30.
 */
public record ProductStatusUpdateRequest(
        @NotNull(message = "상품 상태 입력은 필수입니다")
        ProductStatus status
        ) {
}
