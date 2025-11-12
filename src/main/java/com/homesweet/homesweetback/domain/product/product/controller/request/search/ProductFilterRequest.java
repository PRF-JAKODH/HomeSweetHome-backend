package com.homesweet.homesweetback.domain.product.product.controller.request.search;

import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * 상품 검색 필터 요청 DTO
 *
 * @author junnukim1007gmail.com
 */
@Builder
public record ProductFilterRequest(
        Long categoryId,

        String keyword,

        Map<String, List<String>> optionFilters
) {
    // null-safe 생성자
    public ProductFilterRequest {
        if (optionFilters == null) {
            optionFilters = Map.of();
        }
    }

    public boolean hasOptionFilters() {
        return optionFilters != null && !optionFilters.isEmpty();
    }
}
