package com.homesweet.homesweetback.domain.product.product.repository.jpa.querydsl;

import com.homesweet.homesweetback.common.config.QueryDslConfig;
import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.category.service.cache.CacheCategory;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.ProductDetailResponse;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.ProductManageResponse;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.SkuStockResponse;
import com.homesweet.homesweetback.domain.product.product.command.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.querydsl.CustomProductRepositoryImpl;
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

    @MockitoBean
    private CacheCategory cacheCategory;

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