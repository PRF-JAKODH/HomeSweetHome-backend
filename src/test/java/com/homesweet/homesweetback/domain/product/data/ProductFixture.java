package com.homesweet.homesweetback.domain.product.data;

import com.homesweet.homesweetback.domain.product.product.controller.response.ProductDetailResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductManageResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductPreviewResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.SkuStockResponse;
import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 테스트를 위한 Product 관련 객체 생성
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 7.
 */
public class ProductFixture {

    // 제품 응답 DTO
    public static ProductPreviewResponse createMockProduct(Long id, String name, String brand, Integer price) {
        return new ProductPreviewResponse(
                id,
                1L,
                1L,
                name,
                "https://s3.aws/" + name + ".jpg",
                brand,
                price,
                new BigDecimal("10.0"),
                name + " 상세 설명",
                3000,
                ProductStatus.ON_SALE,
                4.5,
                20L,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now()
        );
    }

    // 제품 상세 응답 DTO
    public static ProductDetailResponse createMockDetailResponse(Long id, String name, String brand) {
        return ProductDetailResponse.builder()
                .id(id)
                .categoryId(1L)
                .sellerId(1L)
                .name(name)
                .imageUrl("https://s3.aws/" + name + ".jpg")
                .detailImageUrls(List.of(
                        "https://s3.aws/" + name + "_detail1.jpg",
                        "https://s3.aws/" + name + "_detail2.jpg"
                ))
                .brand(brand)
                .basePrice(100000)
                .discountRate(new BigDecimal("10.00"))
                .discountedPrice(90000)
                .description(name + " 설명")
                .shippingPrice(3000)
                .status(ProductStatus.ON_SALE)
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // 제품 제고 정보 조회 응답 DTO
    public static SkuStockResponse createMockSku(Long skuId, Long stock, Integer adjustment,
                                           String group1, String value1, String group2, String value2) {
        return new SkuStockResponse(
                skuId,
                stock,
                adjustment,
                List.of(
                        new SkuStockResponse.OptionCombinationResponse(group1, value1),
                        new SkuStockResponse.OptionCombinationResponse(group2, value2)
                )
        );
    }

    public static ProductManageResponse createMockProduct(
            Long id, String name, String categoryPath,
            Integer price, BigDecimal discountRate, Long totalStock
    ) {
        return ProductManageResponse.builder()
                .id(id)
                .name(name)
                .imageUrl("https://s3.aws/" + name + ".jpg")
                .categoryPath(categoryPath)
                .basePrice(price)
                .discountRate(discountRate)
                .shippingPrice(3000)
                .totalStock(totalStock)
                .status(ProductStatus.ON_SALE)
                .createdAt(LocalDateTime.now().minusDays(3))
                .build();
    }
}
