package com.homesweet.homesweetback.domain.product.cart.repository.mapper;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.product.cart.domain.Cart;
import com.homesweet.homesweetback.domain.product.cart.repository.jpa.entity.CartEntity;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.SkuEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CartMapper 단위 테스트
 *
 * Entity ↔ Domain 변환 성공 케이스 검증
 */
@DisplayName("CartMapper 단위 테스트")
class CartMapperTest {

    private final CartMapper mapper = new CartMapper();

    @Nested
    @DisplayName("toDomain()")
    class ToDomain {

        @Test
        @DisplayName("Entity → Domain 정상 변환")
        void toDomain_success() {
            // given
            User user = User.builder().id(1L).build();
            SkuEntity sku = SkuEntity.builder().id(100L).build();

            CartEntity entity = CartEntity.builder()
                    .id(10L)
                    .user(user)
                    .sku(sku)
                    .quantity(3)
                    .build();

            // when
            Cart domain = mapper.toDomain(entity);

            // then
            assertThat(domain).isNotNull();
            assertThat(domain.id()).isEqualTo(10L);
            assertThat(domain.userId()).isEqualTo(1L);
            assertThat(domain.skuId()).isEqualTo(100L);
            assertThat(domain.quantity()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("toEntity()")
    class ToEntity {

        @Test
        @DisplayName("Domain → Entity 정상 변환")
        void toEntity_success() {
            // given
            Cart domain = Cart.builder()
                    .id(10L)
                    .userId(1L)
                    .skuId(100L)
                    .quantity(5)
                    .build();

            // when
            CartEntity entity = mapper.toEntity(domain);

            // then
            assertThat(entity).isNotNull();
            assertThat(entity.getUser().getId()).isEqualTo(1L);
            assertThat(entity.getSku().getId()).isEqualTo(100L);
            assertThat(entity.getQuantity()).isEqualTo(5);
        }
    }
}