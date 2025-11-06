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

    //TODO: 이미지에 대한 변경지점을 한곳에서 관리하는게 어떨까?(SRP)
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
