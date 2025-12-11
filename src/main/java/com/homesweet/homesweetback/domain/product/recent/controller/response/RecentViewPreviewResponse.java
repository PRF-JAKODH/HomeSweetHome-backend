<<<<<<<< HEAD:src/main/java/com/homesweet/homesweetback/domain/product/recent/controller/response/RecentViewPreviewResponse.java
package com.homesweet.homesweetback.domain.product.recent.controller.response;

import com.homesweet.homesweetback.domain.product.product.command.controller.response.ProductDetailResponse;
========
package com.homesweet.homesweetback.domain.product.product.command.controller.response;
>>>>>>>> 9de1dca (feat: CQRS에 맞는 폴더 구조 설정):src/main/java/com/homesweet/homesweetback/domain/product/product/command/controller/response/RecentViewPreviewResponse.java

import java.math.BigDecimal;

/**
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 13.
 */
public record RecentViewPreviewResponse(
        Long id,
        String name,
        String imageUrl,
        Integer basePrice,
        BigDecimal discountRate,
        Integer discountedPrice
) {
    public static RecentViewPreviewResponse fromDetail(ProductDetailResponse detail) {

        Integer discounted = null;
        if (detail.basePrice() != null && detail.discountRate() != null) {
            discounted = (int) Math.round(
                    detail.basePrice() * (1 - detail.discountRate().doubleValue() / 100)
            );
        }

        return new RecentViewPreviewResponse(
                detail.id(),
                detail.name(),
                detail.imageUrl(),
                detail.basePrice(),
                detail.discountRate(),
                discounted
        );
    }
}
