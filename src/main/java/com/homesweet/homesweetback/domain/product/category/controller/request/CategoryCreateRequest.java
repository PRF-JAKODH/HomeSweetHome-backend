package com.homesweet.homesweetback.domain.product.category.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * 제품 카테고리 생성 요청 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@Builder
public record CategoryCreateRequest(
        @NotBlank(message = "카테고리 이름은 필수입니다")
        @Size(max = 12, message = "카테고리 이름은 12자를 넘을 수 없습니다")
        String name,
        Long parentId
) {
}
