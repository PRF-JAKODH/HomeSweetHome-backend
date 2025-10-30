package com.homesweet.homesweetback.domain.product.product.controller.request.update;

import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * 상품 기본 정보 업데이트 요청 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 30.
 */
public record ProductBasicInfoUpdateRequest(

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

        // 이미지 관련
        MultipartFile mainImage,

        @Size(max = 5, message = "상세 이미지는 최대 5개까지 업로드 할 수 있습니다")
        List<MultipartFile> detailImages
) {
}
