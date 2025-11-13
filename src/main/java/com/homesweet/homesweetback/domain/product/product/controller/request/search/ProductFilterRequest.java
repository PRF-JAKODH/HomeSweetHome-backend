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

        @Size(max = 100, message = "검색어는 100자 이내로 입력해주세요.")
        String keyword,

        // 일반 옵션 필터 (정확한 값 매칭)
        Map<String, List<String>> optionFilters,

        // 범위 옵션 필터 (숫자 범위)
        Map<String, RangeFilter> rangeFilters
) {
    // null-safe 생성자
    public ProductFilterRequest {
        if (optionFilters == null) {
            optionFilters = Map.of();
        }
        if (rangeFilters == null) {
            rangeFilters = Map.of();
        }
    }

    public boolean hasOptionFilters() {
        return optionFilters != null && !optionFilters.isEmpty();
    }

    public boolean hasRangeFilters() {
        return rangeFilters != null && !rangeFilters.isEmpty();
    }

    /**
     * 범위 필터 DTO
     */
    public record RangeFilter(
            Integer minValue,  // 최소값 (null이면 제한 없음)
            Integer maxValue   // 최대값 (null이면 제한 없음)
    ) {
        public boolean hasMin() {
            return minValue != null;
        }

        public boolean hasMax() {
            return maxValue != null;
        }
    }
}
