package com.homesweet.homesweetback.domain.product.category.repository.mapper;

import com.homesweet.homesweetback.domain.product.category.domain.ProductCategory;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 11.
 */
@DisplayName("ProductCategoryMapper 단위 테스트")
class ProductCategoryMapperTest {

    private final ProductCategoryMapper mapper = new ProductCategoryMapper();

    @Nested
    @DisplayName("toDomain()")
    class ToDomain {

        @Test
        @DisplayName("Entity → Domain 정상 변환")
        void toDomain_success() {
            // given
            LocalDateTime now = LocalDateTime.now();
            ProductCategoryEntity entity = ProductCategoryEntity.builder()
                    .id(1L)
                    .name("가구")
                    .parentId(10L)
                    .depth(1)
                    .build();

            // when
            ProductCategory domain = mapper.toDomain(entity);

            // then
            assertThat(domain).isNotNull();
            assertThat(domain.id()).isEqualTo(1L);
            assertThat(domain.name()).isEqualTo("가구");
            assertThat(domain.parentId()).isEqualTo(10L);
            assertThat(domain.depth()).isEqualTo(1);
        }

        @Test
        @DisplayName("Entity가 null이면 null 반환")
        void toDomain_nullEntity() {
            // when
            ProductCategory result = mapper.toDomain(null);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("toEntity()")
    class ToEntity {

        @Test
        @DisplayName("Domain → Entity 정상 변환")
        void toEntity_success() {
            // given
            ProductCategory domain = ProductCategory.builder()
                    .id(1L)
                    .name("주방용품")
                    .parentId(5L)
                    .depth(2)
                    .build();

            // when
            ProductCategoryEntity entity = mapper.toEntity(domain);

            // then
            assertThat(entity).isNotNull();
            assertThat(entity.getName()).isEqualTo("주방용품");
            assertThat(entity.getParentId()).isEqualTo(5L);
            assertThat(entity.getDepth()).isEqualTo(2);
        }

        @Test
        @DisplayName("Domain이 null이면 null 반환")
        void toEntity_nullDomain() {
            // when
            ProductCategoryEntity result = mapper.toEntity(null);

            // then
            assertThat(result).isNull();
        }
    }
}