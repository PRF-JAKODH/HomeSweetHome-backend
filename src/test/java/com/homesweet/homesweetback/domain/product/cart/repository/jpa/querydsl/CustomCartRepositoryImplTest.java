package com.homesweet.homesweetback.domain.product.cart.repository.jpa.querydsl;

import com.homesweet.homesweetback.common.config.QueryDslConfig;
import com.homesweet.homesweetback.domain.product.cart.controller.response.CartResponse;
import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.category.service.cache.CacheCategory;
import com.homesweet.homesweetback.domain.product.product.repository.mapper.ProductMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 10.
 */
@Import(
        {QueryDslConfig.class,
        ProductMapper.class}
)
@DataJpaTest
@ActiveProfiles("test")
@Sql("/sql/product/product_test_data.sql")
@DisplayName("CustomCartRepositoryImpl 통합 테스트 - H2 기반")
class CustomCartRepositoryImplTest {

    @Autowired
    private CustomCartRepositoryImpl customCartRepository;

    @MockitoBean
    private ProductCategoryRepository productCategoryRepository;

    @MockitoBean
    private CacheCategory cacheCategory;

    @Test
    @DisplayName("첫 페이지 장바구니 항목 조회 (cursor 없음)")
    void findFirstPage() {
        List<CartResponse> results = customCartRepository.findNextCartItems(10L, null, 2);

        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("커서 기반 다음 페이지 조회")
    void findNextPage() {
        List<CartResponse> results = customCartRepository.findNextCartItems(10L, 2L, 2);

        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("회원별 장바구니만 조회")
    void findOnlyMemberCart() {
        List<CartResponse> results = customCartRepository.findNextCartItems(999L, null, 2);
        assertThat(results).isEmpty();
    }

    @Test // 안채호
    @DisplayName("findNextCartItems가 DTO의 가격/ID 필드를 올바르게 계산하여 반환한다.")
    void findNextCartItems_VerifiesDtoProjectionAndPriceCalculation() {

        // --- GIVEN ---
        // 상수 정의 (product_test_data.sql의 내용과 일치)
        Long userId = 10L;
        Long productId = 100L;

        // SKU 400 (cart_id 600)의 정답
        Long skuId_400 = 400L;
        int expectedFinalPrice_400 = 9000; // (10000 * 0.9) + 0
        int expectedTotalPrice_400 = 9000; // 9000 * 1
        int expectedAdjustment_400 = 0;

        // SKU 401 (cart_id 601)의 정답
        Long skuId_401 = 401L;
        int expectedFinalPrice_401 = 14000; // (10000 * 0.9) + 5000
        int expectedTotalPrice_401 = 28000; // 14000 * 2
        int expectedAdjustment_401 = 5000;

        // --- WHEN ---
        // QueryDSL 쿼리 실행
        // SQL 파일에 2개 있으므로, 2개보다 많이 10개로 조회
        List<CartResponse> results = customCartRepository.findNextCartItems(userId, null, 10);

        // --- THEN ---
        // 갯수 검증 (SQL 파일의 user_id=10L인 항목이 2개인지)
        assertThat(results).hasSize(2);

        // SKU 400 (cart_id 600) 검증
        CartResponse cart600 = results.stream()
                .filter(r -> r.skuId().equals(skuId_400))
                .findFirst()
                .orElseThrow(() -> new AssertionError("cart_id 600이 조회되지 않았습니다."));

        assertThat(cart600.finalPrice()).isEqualTo(expectedFinalPrice_400);
        assertThat(cart600.totalPrice()).isEqualTo(expectedTotalPrice_400);
        assertThat(cart600.priceAdjustment()).isEqualTo(expectedAdjustment_400);
        assertThat(cart600.productId()).isEqualTo(productId);

        // SKU 401 (cart_id 601) 검증
        CartResponse cart601 = results.stream()
                .filter(r -> r.skuId().equals(skuId_401))
                .findFirst()
                .orElseThrow(() -> new AssertionError("cart_id 601이 조회되지 않았습니다."));

        assertThat(cart601.finalPrice()).isEqualTo(expectedFinalPrice_401);
        assertThat(cart601.totalPrice()).isEqualTo(expectedTotalPrice_401);
        assertThat(cart601.priceAdjustment()).isEqualTo(expectedAdjustment_401);
        assertThat(cart601.productId()).isEqualTo(productId);
    }
}