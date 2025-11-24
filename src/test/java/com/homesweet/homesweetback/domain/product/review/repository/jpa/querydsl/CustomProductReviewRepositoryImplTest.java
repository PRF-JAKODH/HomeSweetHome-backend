package com.homesweet.homesweetback.domain.product.review.repository.jpa.querydsl;

import com.homesweet.homesweetback.common.config.QueryDslConfig;
import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.category.service.cache.CacheCategory;
import com.homesweet.homesweetback.domain.product.product.command.repository.mapper.ProductMapper;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewResponse;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewStatisticsResponse;
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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 10.
 */
@Import(
        {QueryDslConfig.class,
        ProductMapper.class}
)
@ActiveProfiles("test")
@DataJpaTest
@Sql("/sql/product/product_test_data.sql")
@DisplayName("CustomProductReview 통합 테스트 - H2 기반")
class CustomProductReviewRepositoryImplTest {

    @Autowired
    private CustomProductReviewRepositoryImpl repository;

    @MockitoBean
    private ProductCategoryRepository categoryRepository;

    @MockitoBean
    private CacheCategory cacheCategory;

    @Nested
    @DisplayName("상품 리뷰 조회")
    class FindReviews {

        @Test
        @DisplayName("특정 상품의 리뷰를 최신순으로 조회한다 (cursor 없음)")
        void findFirstPageReviews() {
            // given
            Long productId = 100L;

            // when
            List<ProductReviewResponse> results = repository.findNextReviews(productId, null, 2);

            // then
            assertThat(results).hasSize(2); // 최신순, 상위 2건
            assertThat(results.get(0).comment()).isEqualTo("괜찮아요");
            assertThat(results.get(1).comment()).isEqualTo("좋아요!");
            assertThat(results.get(0).productId()).isEqualTo(productId);
        }

        @Test
        @DisplayName("커서를 기준으로 이전 리뷰를 조회한다 (cursor 적용)")
        void findNextPageReviews() {
            // given
            Long productId = 100L;
            Long cursorId = 2L; // 최신 리뷰 id 2 이후 → 1번 리뷰만 조회

            // when
            List<ProductReviewResponse> results = repository.findNextReviews(productId, cursorId, 2);

            // then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("다른 상품의 리뷰를 조회하면 해당 상품만 조회된다")
        void findReviewsByOtherProduct() {
            // given
            Long productId = 102L;

            // when
            List<ProductReviewResponse> results = repository.findNextReviews(productId, null, 5);

            // then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("리뷰가 존재하지 않는 상품은 빈 리스트를 반환한다")
        void findEmptyReviews() {
            // given
            Long productId = 103L;

            // when
            List<ProductReviewResponse> results = repository.findNextReviews(productId, null, 3);

            // then
            assertThat(results).isEmpty();
        }

    }

    @Nested
    @DisplayName("사용자가 작성한 리뷰 조회")
    class FindUserReviews {
        @Test
        @DisplayName("특정 유저가 작성한 리뷰를 최신순으로 조회한다 (cursor 없음)")
        void findFirstPageUserReviews() {
            // given
            Long userId = 10L; // 판매자A가 작성한 리뷰 → review_id=1

            // when
            List<ProductReviewResponse> results = repository.findNextUserReviews(userId, null, 5);

            // then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("커서 기준으로 다음 리뷰 페이지를 조회한다 (cursor 적용)")
        void findNextPageUserReviews() {
            // given
            Long userId = 11L;
            Long cursorId = 2L;

            // when
            List<ProductReviewResponse> results = repository.findNextUserReviews(userId, cursorId, 5);

            // then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("유저가 작성하지 않은 경우 빈 리스트를 반환한다")
        void findUserWithoutReviews() {
            // given
            Long userId = 999L; // 존재하지 않는 유저

            // when
            List<ProductReviewResponse> results = repository.findNextUserReviews(userId, null, 5);

            // then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("다른 유저의 리뷰는 포함되지 않는다")
        void excludeOtherUserReviews() {
            // given
            Long userId = 12L;

            // when
            List<ProductReviewResponse> results = repository.findNextUserReviews(userId, null, 5);

            // then
            assertThat(results).hasSize(1);
        }
    }

    @Nested
    @DisplayName("리뷰 통계 정보 조회")
    class GetReviewStatistics {
        @Test
        @DisplayName("상품 100의 리뷰 통계를 조회한다 (총 2건, 평균 4.5)")
        void getStatisticsForProduct100() {
            // given
            Long productId = 100L;

            // when
            ProductReviewStatisticsResponse result = repository.getReviewStatistics(productId);

            // then
            assertThat(result.productId()).isEqualTo(productId);
            assertThat(result.totalCount()).isEqualTo(2);
            assertThat(result.averageRating()).isEqualTo(4.5);
        }

        @Test
        @DisplayName("상품 102의 리뷰 통계를 조회한다 (총 1건, 평균 3.0)")
        void getStatisticsForProduct102() {
            // given
            Long productId = 102L;

            // when
            ProductReviewStatisticsResponse result = repository.getReviewStatistics(productId);

            // then
            assertThat(result.productId()).isEqualTo(productId);
            assertThat(result.totalCount()).isEqualTo(1);
            assertThat(result.averageRating()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("리뷰가 존재하지 않는 상품은 0으로 조회된다")
        void getStatisticsForProductWithoutReviews() {
            // given
            Long productId = 103L; // 리뷰 없음

            // when
            ProductReviewStatisticsResponse result = repository.getReviewStatistics(productId);

            // then
            assertThat(result.totalCount()).isZero();
            assertThat(result.averageRating()).isEqualTo(0.0);

            result.ratingCounts().values().forEach(count -> assertThat(count).isZero());
        }
    }
}