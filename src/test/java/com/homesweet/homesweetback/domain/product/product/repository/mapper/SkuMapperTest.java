package com.homesweet.homesweetback.domain.product.product.repository.mapper;

import com.homesweet.homesweetback.domain.product.product.command.domain.Sku;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.SkuEntity;
import com.homesweet.homesweetback.domain.product.product.command.repository.mapper.SkuMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SkuMapper 단위 테스트
 *
 * Entity ↔ Domain 변환 검증
 */
@DisplayName("SkuMapper 단위 테스트")
class SkuMapperTest {

    private final SkuMapper mapper = new SkuMapper();

    @Nested
    @DisplayName("toDomain()")
    class ToDomain {

        @Test
        @DisplayName("Entity → Domain 정상 변환")
        void toDomain_success() {
            // given
            SkuEntity entity = SkuEntity.builder()
                    .id(1L)
                    .priceAdjustment(5000)
                    .stockQuantity(20L)
                    .build();

            // when
            Sku domain = mapper.toDomain(entity);

            // then
            assertThat(domain).isNotNull();
            assertThat(domain.getId()).isEqualTo(1L);
            assertThat(domain.getPriceAdjustment()).isEqualTo(5000);
            assertThat(domain.getStockQuantity()).isEqualTo(20L);
        }
    }

    @Nested
    @DisplayName("toEntity()")
    class ToEntity {

        @Test
        @DisplayName("Domain → Entity 정상 변환")
        void toEntity_success() {
            // given
            Sku domain = Sku.builder()
                    .id(1L)
                    .priceAdjustment(3000)
                    .stockQuantity(15L)
                    .build();

            // when
            SkuEntity entity = mapper.toEntity(domain);

            // then
            assertThat(entity).isNotNull();
            assertThat(entity.getPriceAdjustment()).isEqualTo(3000);
            assertThat(entity.getStockQuantity()).isEqualTo(15L);
        }
    }
}