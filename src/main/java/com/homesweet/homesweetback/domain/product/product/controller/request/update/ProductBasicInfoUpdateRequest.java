package com.homesweet.homesweetback.domain.product.product.controller.request.update;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 상품 기본 정보 업데이트 요청 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 30.
 */
public record ProductBasicInfoUpdateRequest(

        @Size(max = 30, message = "제품명은 30자 이내로 입력해주세요.")
        String name,

        @Size(max = 20, message = "브랜드는 20자 이내로 입력해주세요.")
        String brand,

        @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
        Integer basePrice,

        @DecimalMax(value = "100.00", message = "할인율은 100% 이하여야 합니다.")
        BigDecimal discountRate,

        @Size(max = 255, message = "설명은 255자 이내로 입력해주세요.")
        String description,

        @Min(value = 0, message = "배송비는 0원 이상이어야 합니다.")
        Integer shippingPrice

) {
}
