package com.homesweet.homesweetback.domain.product.product.controller.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 제품 등록 요청 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
public record ProductCreateRequest(

        @NotNull(message = "카테고리 ID는 필수입니다.")
        Long categoryId,

        @NotBlank(message = "제품명은 필수입니다.")
        @Size(max = 30, message = "제품명은 30자 이내로 입력해주세요.")
        String name,

        @NotBlank(message = "브랜드는 필수입니다.")
        @Size(max = 20, message = "브랜드는 20자 이내로 입력해주세요.")
        String brand,

        @NotNull(message = "기본 가격은 필수입니다.")
        @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
        Integer basePrice,

        @DecimalMin(value = "0.00", message = "할인율은 0% 이상이어야 합니다.")
        @DecimalMax(value = "100.00", message = "할인율은 100% 이하여야 합니다.")
        BigDecimal discountRate,

        @Size(max = 255, message = "설명은 255자 이내로 입력해주세요.")
        String description,

        @NotNull(message = "배송비는 필수입니다.")
        @Min(value = 0, message = "배송비는 0원 이상이어야 합니다.")
        Integer shippingPrice,

        List<ProductOptionGroupRequest> optionGroups,

        List<SkuRequest> skus
) {

    /**
     * 제품 옵션 요청 DTO
     */
    public record ProductOptionGroupRequest(
            @NotBlank(message = "옵션명은 필수입니다.")
            @Size(max = 12, message = "옵션명은 12자 이내로 입력해주세요.")
            String groupName,

            List<String> values
    ) {}

    /**
     * SKU 요청 DTO
     */
    public record SkuRequest(
            Integer priceAdjustment,

            @NotNull(message = "재고 수량은 필수입니다.")
            @Min(value = 0, message = "재고는 0개 이상이어야 합니다.")
            Long stockQuantity,

            List<Integer> optionIndexes
    ) {}
}
