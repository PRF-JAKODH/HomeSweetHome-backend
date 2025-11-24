package com.homesweet.homesweetback.domain.product.review.repository.mapper;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.review.domain.ProductReview;
import com.homesweet.homesweetback.domain.product.review.repository.jpa.entity.ProductReviewEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 11.
 */
@DisplayName("ProductReviewMapper 단위 테스트")
class ProductReviewMapperTest {

    private final ProductReviewMapper mapper = new ProductReviewMapper();

    @Nested
    @DisplayName("toDomain()")
    class ToDomain {

        @Test
        @DisplayName("Entity → Domain 정상 변환")
        void toDomain_success() {
            // given
            ProductEntity product = ProductEntity.builder().id(10L).build();
            User user = User.builder().id(100L).build();

            ProductReviewEntity entity = ProductReviewEntity.builder()
                    .id(1L)
                    .product(product)
                    .user(user)
                    .rating(5)
                    .comment("좋은 제품이에요")
                    .imageUrl("https://s3.aws/review.jpg")
                    .build();

            // when
            ProductReview domain = mapper.toDomain(entity);

            // then
            assertThat(domain).isNotNull();
            assertThat(domain.id()).isEqualTo(1L);
            assertThat(domain.productId()).isEqualTo(10L);
            assertThat(domain.userId()).isEqualTo(100L);
            assertThat(domain.rating()).isEqualTo(5);
            assertThat(domain.comment()).isEqualTo("좋은 제품이에요");
            assertThat(domain.imageUrl()).isEqualTo("https://s3.aws/review.jpg");
        }
    }

    @Nested
    @DisplayName("toEntity()")
    class ToEntity {

        @Test
        @DisplayName("Domain → Entity 정상 변환")
        void toEntity_success() {
            // given
            ProductReview domain = ProductReview.builder()
                    .id(1L)
                    .productId(10L)
                    .userId(100L)
                    .rating(4)
                    .comment("좋지만 배송이 느려요")
                    .imageUrl("https://s3.aws/review2.jpg")
                    .build();

            // when
            ProductReviewEntity entity = mapper.toEntity(domain);

            // then
            assertThat(entity).isNotNull();
            assertThat(entity.getProduct().getId()).isEqualTo(10L);
            assertThat(entity.getUser().getId()).isEqualTo(100L);
            assertThat(entity.getRating()).isEqualTo(4);
            assertThat(entity.getComment()).isEqualTo("좋지만 배송이 느려요");
            assertThat(entity.getImageUrl()).isEqualTo("https://s3.aws/review2.jpg");
        }
    }
}