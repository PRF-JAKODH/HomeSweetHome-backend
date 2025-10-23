package com.homesweet.homesweetback.domain.product.product.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 제품 상세 이미지 도메인
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
@Getter
@Builder
public class ProductDetailImage {

    private Long id;
    private Long productId;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static List<ProductDetailImage> createDetailImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return new ArrayList<>();
        }

        return imageUrls.stream()
                .map(url -> ProductDetailImage.builder()
                        .imageUrl(url)
                        .build())
                .toList();
    }
}
