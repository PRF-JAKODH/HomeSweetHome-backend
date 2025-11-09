package com.homesweet.homesweetback.domain.product.product.repository.jpa.querydsl;

import com.homesweet.homesweetback.common.config.QueryDslConfig;
import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductPreviewResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.SkuStockResponse;
import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 9.
 */
@Import(QueryDslConfig.class)
@DataJpaTest
@ActiveProfiles("test")
@Sql("/sql/product/product_test_data.sql")
@DisplayName("CustomProductRepository 통합 테스트 - H2 기반")
class CustomProductRepositoryImplTest {

    @Autowired
    private CustomProductRepositoryImpl repository;

    @MockitoBean
    private ProductCategoryRepository categoryRepository;

    @Nested
    @DisplayName("상품 무한 스크롤 조회 (findNextProducts)")
    class FindNextProducts {

        @Test
        @DisplayName("최신순 정렬로 상품을 조회할 수 있다")
        void findLatestProducts() {
            List<ProductPreviewResponse> results =
                    repository.findNextProducts(null, 1L, 10, null, ProductSortType.LATEST);

            assertThat(results).isNotEmpty();
            assertThat(results).allSatisfy(r -> assertThat(r.status()).isNotEqualTo(ProductStatus.SUSPENDED));
            assertThat(results).extracting(ProductPreviewResponse::name)
                    .containsExactlyInAnyOrder("고급 의자", "저가 의자", "책상 세트");
        }

        @Test
        @DisplayName("가격 오름차순 정렬로 조회할 수 있다")
        void findPriceLowToHigh() {
            List<ProductPreviewResponse> results =
                    repository.findNextProducts(null, 1L, 10, null, ProductSortType.PRICE_LOW);

            assertThat(results).isNotEmpty();
            assertThat(results.getFirst().basePrice()).isLessThanOrEqualTo(results.getLast().basePrice());
        }

        @Test
        @DisplayName("가격 내림차순 정렬로 조회할 수 있다")
        void findPriceHighToLow() {
            List<ProductPreviewResponse> results =
                    repository.findNextProducts(null, 1L, 10, null, ProductSortType.PRICE_HIGH);

            assertThat(results).isNotEmpty();
            assertThat(results.getFirst().basePrice()).isGreaterThanOrEqualTo(results.getLast().basePrice());

            int maxPrice = results.stream()
                    .mapToInt(ProductPreviewResponse::basePrice)
                    .max()
                    .orElseThrow();
            assertThat(results.getFirst().basePrice()).isEqualTo(maxPrice);
        }

        @Test
        @DisplayName("인기순(리뷰 개수 순)으로 상품을 조회할 수 있다")
        void findByPopularity() {
            List<ProductPreviewResponse> results =
                    repository.findNextProducts(null, 1L, 10, null, ProductSortType.POPULAR);

            assertThat(results).isNotEmpty();

            Long previousReviewCount = Long.MAX_VALUE;
            for (ProductPreviewResponse response : results) {
                assertThat(response.reviewCount()).isLessThanOrEqualTo(previousReviewCount);
                previousReviewCount = response.reviewCount();
            }

            // 고급 의자 상품에 리뷰가 2개로 가장 많음
            ProductPreviewResponse mostPopular = results.getFirst();
            assertThat(mostPopular.name()).isEqualTo("고급 의자");
            assertThat(mostPopular.reviewCount()).isEqualTo(2L);
        }

        @Test
        @DisplayName("검색 키워드가 있으면 제품명 또는 브랜드로 검색된다")
        void findByKeyword() {
            List<ProductPreviewResponse> results =
                    repository.findNextProducts(null, 1L, 10, "홈스윗", ProductSortType.LATEST);

            assertThat(results).isNotEmpty();
            assertThat(results).allSatisfy(p ->
                    assertThat(p.brand()).contains("홈스윗")
            );
        }

        @Test
        @DisplayName("판매 중지 상품은 조회되지 않는다")
        void excludeSuspendedProducts() {
            List<ProductPreviewResponse> results =
                    repository.findNextProducts(null, 1L, 10, null, ProductSortType.LATEST);

            assertThat(results).noneMatch(p -> p.status() == ProductStatus.SUSPENDED);
        }

        @Test
        @DisplayName("카테고리를 선택하면 하위 카테고리 상품도 함께 조회된다")
        void includeSubCategoryProducts() {
            List<ProductPreviewResponse> results =
                    repository.findNextProducts(null, 1L, 10, null, ProductSortType.LATEST);

            assertThat(results).extracting(ProductPreviewResponse::categoryId)
                    .contains(2L, 3L);
        }
    }

    @Nested
    @DisplayName("상품 옵션 조합 별 추가 금액 및 재고 조회")
    class FindProductOptionCombinations {
        @Test
        @DisplayName("상품 ID로 SKU별 옵션 조합과 재고를 조회할 수 있다")
        void findSkuStocksByProductId_success() {
            // given
            Long productId = 100L;

            // when
            List<SkuStockResponse> results = repository.findSkuStocksByProductId(productId);

            // then
            assertThat(results).hasSize(2);

            SkuStockResponse sku1 = results.get(0);
            SkuStockResponse sku2 = results.get(1);

            // SKU1: 화이트 + S
            assertThat(sku1.skuId()).isEqualTo(400L);
            assertThat(sku1.stockQuantity()).isEqualTo(10L);
            assertThat(sku1.priceAdjustment()).isEqualTo(0);
            assertThat(sku1.options())
                    .extracting(SkuStockResponse.OptionCombinationResponse::groupName)
                    .containsExactlyInAnyOrder("색상", "사이즈");
            assertThat(sku1.options())
                    .extracting(SkuStockResponse.OptionCombinationResponse::valueName)
                    .containsExactlyInAnyOrder("화이트", "S");

            // SKU2: 블랙 + L
            assertThat(sku2.skuId()).isEqualTo(401L);
            assertThat(sku2.stockQuantity()).isEqualTo(5L);
            assertThat(sku2.priceAdjustment()).isEqualTo(5000);
            assertThat(sku2.options())
                    .extracting(SkuStockResponse.OptionCombinationResponse::valueName)
                    .containsExactlyInAnyOrder("블랙", "L");
        }

        @Test
        @DisplayName("상품에 SKU가 없으면 빈 리스트를 반환한다")
        void findSkuStocksByProductId_empty() {
            // given
            Long productId = 999L;

            // when
            List<SkuStockResponse> results = repository.findSkuStocksByProductId(productId);

            // then
            assertThat(results).isEmpty();
        }
    }
}