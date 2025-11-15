package com.homesweet.homesweetback.domain.product.cart.repository.jpa.querydsl;

import com.homesweet.homesweetback.common.config.QueryDslConfig;
import com.homesweet.homesweetback.domain.product.cart.controller.response.CartResponse;
import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.product.repository.mapper.ProductMapper;
import org.junit.jupiter.api.DisplayName;
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
}