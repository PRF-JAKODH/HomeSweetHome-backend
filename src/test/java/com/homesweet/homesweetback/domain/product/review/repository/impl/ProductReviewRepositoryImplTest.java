package com.homesweet.homesweetback.domain.product.review.repository.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.product.product.command.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewResponse;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewStatisticsResponse;
import com.homesweet.homesweetback.domain.product.review.domain.ProductReview;
import com.homesweet.homesweetback.domain.product.review.repository.jpa.ProductReviewJPARepository;
import com.homesweet.homesweetback.domain.product.review.repository.jpa.entity.ProductReviewEntity;
import com.homesweet.homesweetback.domain.product.review.repository.mapper.ProductReviewMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.homesweet.homesweetback.domain.product.data.ProductReviewMockData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 11.
 */
@ExtendWith(MockitoExtension.class)
class ProductReviewRepositoryImplTest {

    @InjectMocks
    private ProductReviewRepositoryImpl repository;

    @Mock
    private ProductReviewJPARepository jpaRepository;
    @Mock
    private ProductReviewMapper mapper;

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("리뷰 저장 성공 시 매퍼와 JPA save가 정상 호출된다")
        void save_success() {
            // given
            ProductReview domain = createMockReview(1L, 1L, 1L, null);

            ProductReviewEntity entity = ProductReviewEntity.builder().id(1L).build();

            given(mapper.toEntity(domain)).willReturn(entity);
            given(jpaRepository.save(entity)).willReturn(entity);
            given(mapper.toDomain(entity)).willReturn(domain);

            // when
            ProductReview result = repository.save(domain);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            verify(jpaRepository).save(entity);
            verify(mapper).toDomain(entity);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("존재하는 리뷰를 조회하면 매퍼로 변환된 도메인을 반환한다")
        void findById_success() {
            // given
            ProductReviewEntity entity = ProductReviewEntity.builder().id(1L).build();
            ProductReview domain = ProductReview.builder().id(1L).build();

            given(jpaRepository.findById(1L)).willReturn(Optional.of(entity));
            given(mapper.toDomain(entity)).willReturn(domain);

            // when
            Optional<ProductReview> result = repository.findById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(1L);
            verify(mapper).toDomain(entity);
        }

        @Test
        @DisplayName("존재하지 않는 리뷰 조회 시 빈 Optional 반환")
        void findById_empty() {
            // given
            given(jpaRepository.findById(999L)).willReturn(Optional.empty());

            // when
            Optional<ProductReview> result = repository.findById(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByProductIdAndUserId()")
    class ExistsByProductIdAndUserId {

        @Test
        @DisplayName("리뷰 존재 여부를 정상적으로 반환한다")
        void exists_success() {
            // given
            given(jpaRepository.existsByProductIdAndUserId(10L, 100L)).willReturn(true);

            // when
            boolean result = repository.existsByProductIdAndUserId(10L, 100L);

            // then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("findNextReviews()")
    class FindNextReviews {

        @Test
        @DisplayName("제품 ID와 커서로 다음 리뷰 목록을 조회한다")
        void findNextReviews_success() {
            // given
            List<ProductReviewResponse> mockList = List.of(
                    createReviewResponse(1L, 1L, 1L, 5, "좋아요1"),
                    createReviewResponse(2L, 1L, 2L, 4, "좋아요2")
            );

            given(jpaRepository.findNextReviews(10L, 0L, 2)).willReturn(mockList);

            // when
            List<ProductReviewResponse> result = repository.findNextReviews(10L, 0L, 2);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.getFirst().comment()).isEqualTo("좋아요1");
        }
    }

    @Nested
    @DisplayName("findNextUserReviews()")
    class FindNextUserReviews {

        @Test
        @DisplayName("유저 ID와 커서로 다음 리뷰 목록을 조회한다")
        void findNextUserReviews_success() {
            // given
            List<ProductReviewResponse> mockList = List.of(
                    createReviewResponse(1L, 1L, 1L, 5, "좋아요1"),
                    createReviewResponse(2L, 1L, 2L, 4, "좋아요2")
            );

            given(jpaRepository.findNextUserReviews(100L, 0L, 2)).willReturn(mockList);

            // when
            List<ProductReviewResponse> result = repository.findNextUserReviews(100L, 0L, 2);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.getFirst().rating()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("존재하는 리뷰는 수정 후 도메인으로 반환된다")
        void update_success() {
            // given
            ProductReviewEntity entity = ProductReviewEntity.builder()
                    .id(1L).rating(3).comment("old").imageUrl("old.jpg").build();

            ProductReview domain = ProductReview.builder()
                    .id(1L).rating(5).comment("new").imageUrl("new.jpg").build();

            given(jpaRepository.findById(1L)).willReturn(Optional.of(entity));
            given(mapper.toDomain(entity)).willReturn(domain);

            // when
            ProductReview result = repository.update(domain);

            // then
            assertThat(result.comment()).isEqualTo("new");
            assertThat(result.rating()).isEqualTo(5);
        }

        @Test
        @DisplayName("존재하지 않는 리뷰 수정 시 예외 발생")
        void update_notFound() {
            // given
            ProductReview domain = ProductReview.builder().id(99L).build();
            given(jpaRepository.findById(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> repository.update(domain))
                    .isInstanceOf(ProductException.class)
                    .hasMessageContaining(ErrorCode.PRODUCT_REVIEW_NOT_FOUND_ERROR.getMessage());
        }
    }

    @Nested
    @DisplayName("getReviewStatistics()")
    class GetReviewStatistics {

        @Test
        @DisplayName("제품 ID로 리뷰 통계 조회 시 정상 반환된다")
        void getReviewStatistics_success() {
            // given
            ProductReviewStatisticsResponse mockResponse =
                    createReviewStatisticsResponse(1L);

            given(jpaRepository.getReviewStatistics(10L)).willReturn(mockResponse);

            // when
            ProductReviewStatisticsResponse result = repository.getReviewStatistics(10L);

            // then
            assertThat(result.averageRating()).isEqualTo(4.5);
            assertThat(result.totalCount()).isEqualTo(10L);
        }
    }
}