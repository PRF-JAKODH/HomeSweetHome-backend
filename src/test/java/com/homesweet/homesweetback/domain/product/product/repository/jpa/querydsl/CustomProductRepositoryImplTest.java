package com.homesweet.homesweetback.domain.product.product.repository.jpa.querydsl;

import com.homesweet.homesweetback.common.config.QueryDslConfig;
import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductDetailResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductManageResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductPreviewResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.SkuStockResponse;
import com.homesweet.homesweetback.domain.product.product.domain.Product;
import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.product.repository.mapper.ProductMapper;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 9.
 */
@Import(
        {QueryDslConfig.class,
        ProductMapper.class}
)
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
            List<Product> results =
                    repository.findNextProducts(null, 1L, 10, null, ProductSortType.LATEST);

            assertThat(results).isNotEmpty();
            assertThat(results).allSatisfy(r -> assertThat(r.getStatus()).isNotEqualTo(ProductStatus.SUSPENDED));
            assertThat(results).extracting(Product::getName)
                    .containsExactlyInAnyOrder("고급 의자", "저가 의자", "책상 세트");
        }

        @Test
        @DisplayName("가격 오름차순 정렬로 조회할 수 있다")
        void findPriceLowToHigh() {
            List<Product> results =
                    repository.findNextProducts(null, 1L, 10, null, ProductSortType.PRICE_LOW);

            assertThat(results).isNotEmpty();
            assertThat(results.getFirst().getBasePrice()).isLessThanOrEqualTo(results.getLast().getBasePrice());
        }

        @Test
        @DisplayName("가격 내림차순 정렬로 조회할 수 있다")
        void findPriceHighToLow() {
            List<Product> results =
                    repository.findNextProducts(null, 1L, 10, null, ProductSortType.PRICE_HIGH);

            assertThat(results).isNotEmpty();
            assertThat(results.getFirst().getBasePrice()).isGreaterThanOrEqualTo(results.getLast().getBasePrice());

            int maxPrice = results.stream()
                    .mapToInt(Product::getBasePrice)
                    .max()
                    .orElseThrow();
            assertThat(results.getFirst().getBasePrice()).isEqualTo(maxPrice);
        }

        @Test
        @DisplayName("인기순(리뷰 개수 순)으로 상품을 조회할 수 있다")
        void findByPopularity() {
            List<Product> results =
                    repository.findNextProducts(null, 1L, 10, null, ProductSortType.POPULAR);

            assertThat(results).isNotEmpty();

            Long previousReviewCount = Long.MAX_VALUE;

            // 고급 의자 상품에 리뷰가 2개로 가장 많음
            Product mostPopular = results.getFirst();
            assertThat(mostPopular.getName()).isEqualTo("고급 의자");
        }

        @Test
        @DisplayName("검색 키워드가 있으면 제품명 또는 브랜드로 검색된다")
        void findByKeyword() {
            List<Product> results =
                    repository.findNextProducts(null, 1L, 10, "홈스윗", ProductSortType.LATEST);

            assertThat(results).isNotEmpty();
            assertThat(results).allSatisfy(p ->
                    assertThat(p.getBrand()).contains("홈스윗")
            );
        }

        @Test
        @DisplayName("판매 중지 상품은 조회되지 않는다")
        void excludeSuspendedProducts() {
            List<Product> results =
                    repository.findNextProducts(null, 1L, 10, null, ProductSortType.LATEST);

            assertThat(results).noneMatch(p -> p.getStatus() == ProductStatus.SUSPENDED);
        }

        @Test
        @DisplayName("카테고리를 선택하면 하위 카테고리 상품도 함께 조회된다")
        void includeSubCategoryProducts() {
            List<Product> results =
                    repository.findNextProducts(null, 1L, 10, null, ProductSortType.LATEST);

            assertThat(results).extracting(Product::getCategoryId)
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

    @Nested
    @DisplayName("상품 상세 정보 조회")
    class FindProductDetailById {

        @Test
        @DisplayName("상품 ID로 상세 정보를 조회할 수 있다")
        void findProductDetailById_success() {
            // given
            Long productId = 100L; // SQL 파일의 "고급 의자"

            // when
            Optional<ProductDetailResponse> result = repository.findProductDetailById(productId);

            // then
            assertThat(result).isPresent();
            ProductDetailResponse response = result.get();

            // 기본 필드 검증
            assertThat(response.id()).isEqualTo(100L);
            assertThat(response.name()).isEqualTo("고급 의자");
            assertThat(response.brand()).isEqualTo("홈스윗");
            assertThat(response.imageUrl()).isEqualTo("https://a.jpg");
            assertThat(response.description()).isEqualTo("좋은 의자");

            // 가격 관련 검증
            assertThat(response.basePrice()).isEqualTo(10000);
            assertThat(response.discountRate()).isEqualTo(new BigDecimal("10.00"));
            assertThat(response.discountedPrice()).isEqualTo((int) Math.round(10000 * (1 - 0.1))); // 9000
            assertThat(response.shippingPrice()).isEqualTo(3000);

            // 관계 검증
            assertThat(response.categoryId()).isEqualTo(2L);
            assertThat(response.sellerId()).isEqualTo(10L);
            assertThat(response.status()).isEqualTo(ProductStatus.ON_SALE);

            // 상세 이미지 검증
            assertThat(response.detailImageUrls())
                    .containsExactly("https://a_detail_1.jpg", "https://a_detail_2.jpg");
        }

        @Test
        @DisplayName("상품 ID가 존재하지 않으면 Optional.empty()를 반환한다")
        void findProductDetailById_notFound() {
            // given
            Long productId = 9999L;

            // when
            Optional<ProductDetailResponse> result = repository.findProductDetailById(productId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("판매자 상품 관리 목록 조회")
    class FindProductsForSeller {

        @Test
        @DisplayName("특정 판매자는 본인이 등록한 모든 상품(판매 중지 포함)을 조회할 수 있다")
        void findProductsForSeller_allStatusesIncluded() {
            // given
            Long sellerId = 11L; // 판매자B
            String startDate = "2020-01-01";
            String endDate = "2030-01-01";

            // when
            List<ProductManageResponse> results = repository.findProductsForSeller(sellerId, startDate, endDate);

            // then
            assertThat(results).hasSize(2);
            assertThat(results)
                    .extracting(ProductManageResponse::name)
                    .containsExactlyInAnyOrder("책상 세트", "판매 중지 상품");

            // 카테고리 경로 검증
            assertThat(results.get(0).categoryPath()).contains("가구 > 책상");
        }

        @Test
        @DisplayName("날짜 범위를 지정하면 해당 기간 내 등록된 상품만 조회된다")
        void findProductsForSeller_withDateRange() {
            // given
            Long sellerId = 11L;
            String startDate = LocalDate.now().minusDays(2).toString(); // 2일 전
            String endDate = LocalDate.now().toString(); // 오늘까지

            // when
            List<ProductManageResponse> results = repository.findProductsForSeller(sellerId, startDate, endDate);

            // then
            assertThat(results)
                    .extracting(ProductManageResponse::name)
                    .containsExactlyInAnyOrder("책상 세트", "판매 중지 상품");
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 빈 리스트를 반환한다")
        void findProductsForSeller_noProducts() {
            // given
            Long sellerId = 99L; // 존재하지 않는 판매자
            String startDate = "2020-01-01";
            String endDate = "2030-01-01";

            // when
            List<ProductManageResponse> results = repository.findProductsForSeller(sellerId, startDate, endDate);

            // then
            assertThat(results).isEmpty();
        }
    }
}