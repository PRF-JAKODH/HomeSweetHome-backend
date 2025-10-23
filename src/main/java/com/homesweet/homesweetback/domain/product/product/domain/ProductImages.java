package com.homesweet.homesweetback.domain.product.product.domain;

import java.util.List;

/**
 * 제품에 들어가는 이미지들 관련 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
public record ProductImages(
        String mainImageUrl,
        List<String> detailImageUrls
) {
}
