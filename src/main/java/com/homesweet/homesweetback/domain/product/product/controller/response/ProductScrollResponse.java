package com.homesweet.homesweetback.domain.product.product.controller.response;

import java.util.List;

/**
 * 제품 무한 스크롤 응답 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
public record ProductScrollResponse(
        List<ProductPreviewResponse> products,
        Long nextCursorId,
        boolean hasNext
) {
}