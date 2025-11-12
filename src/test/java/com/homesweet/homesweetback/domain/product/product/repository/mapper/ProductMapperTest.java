package com.homesweet.homesweetback.domain.product.product.repository.mapper;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.product.domain.*;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ProductMapper 단위 테스트")
class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapper();

    @Nested
    @DisplayName("Entity → Domain 변환")
    class ToDomain {

        @Test
        @DisplayName("정상 변환 시 모든 필드가 매핑된다")
        void toDomain_success() {
            // given
            ProductCategoryEntity category = ProductCategoryEntity.builder().id(10L).build();
            User seller = User.builder().id(20L).build();

            ProductOptionValueEntity optionValue = ProductOptionValueEntity.builder()
                    .id(100L).value("화이트").build();

            ProductOptionGroupEntity optionGroup = ProductOptionGroupEntity.builder()
                    .id(200L)
                    .groupName("색상")
                    .values(List.of(optionValue))
                    .build();

            SkuEntity sku = SkuEntity.builder()
                    .id(300L)
                    .priceAdjustment(5000)
                    .stockQuantity(30L)
                    .skuOptions(List.of(ProductSkuOptionEntity.builder()
                            .optionValue(optionValue)
                            .build()))
                    .build();

            ProductDetailImageEntity detailImage = ProductDetailImageEntity.builder()
                    .id(400L)
                    .imageUrl("https://s3.aws/test.jpg")
                    .build();

            ProductEntity entity = ProductEntity.builder()
                    .id(1L)
                    .category(category)
                    .seller(seller)
                    .name("테스트상품")
                    .brand("홈스윗")
                    .imageUrl("https://s3.aws/main.jpg")
                    .basePrice(10000)
                    .discountRate(BigDecimal.valueOf(10))
                    .description("상품 설명")
                    .shippingPrice(3000)
                    .status(ProductStatus.ON_SALE)
                    .build();

            entity.addDetailImage(detailImage);
            entity.addOption(optionGroup);
            entity.addSku(sku);

            // when
            Product result = mapper.toDomain(entity);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCategoryId()).isEqualTo(10L);
            assertThat(result.getSellerId()).isEqualTo(20L);
            assertThat(result.getDescription()).isEqualTo("상품 설명");
            assertThat(result.getOptionGroups()).hasSize(1);
            assertThat(result.getSkus()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Domain → Entity 변환")
    class ToEntity {

        @Test
        @DisplayName("정상 변환 시 모든 서브 엔티티가 cascade로 추가된다")
        void toEntity_success() {
            // given
            Product domain = Product.builder()
                    .id(1L)
                    .categoryId(10L)
                    .sellerId(20L)
                    .name("테스트상품")
                    .brand("홈스윗")
                    .imageUrl("https://s3.aws/main.jpg")
                    .basePrice(20000)
                    .discountRate(BigDecimal.valueOf(15))
                    .description("상품 설명")
                    .shippingPrice(5000)
                    .status(ProductStatus.ON_SALE)
                    .detailImages(List.of(
                            ProductDetailImage.builder().imageUrl("https://s3.aws/test1.jpg").build()
                    ))
                    .optionGroups(List.of(
                            ProductOptionGroup.builder()
                                    .groupName("색상")
                                    .values(List.of(
                                            ProductOptionValue.builder().value("화이트").build(),
                                            ProductOptionValue.builder().value("블랙").build()
                                    ))
                                    .build()
                    ))
                    .skus(List.of(
                            Sku.builder()
                                    .priceAdjustment(3000)
                                    .stockQuantity(20L)
                                    .optionValueIndexes(List.of(0L, 1L))
                                    .build()
                    ))
                    .build();

            // when
            ProductEntity entity = mapper.toEntity(domain);

            // then
            assertThat(entity.getCategory().getId()).isEqualTo(10L);
            assertThat(entity.getSeller().getId()).isEqualTo(20L);
            assertThat(entity.getDetailImages()).hasSize(1);
            assertThat(entity.getOptionGroups()).hasSize(1);
            assertThat(entity.getOptionGroups().get(0).getValues()).hasSize(2);
            assertThat(entity.getSkus()).hasSize(1);

            SkuEntity skuEntity = entity.getSkus().get(0);
            assertThat(skuEntity.getSkuOptions()).hasSize(2);
        }

        @Test
        @DisplayName("옵션 인덱스가 유효 범위를 벗어나면 ProductException 발생")
        void toEntity_invalidOptionIndex() {
            // given
            Product domain = Product.builder()
                    .categoryId(10L)
                    .sellerId(20L)
                    .optionGroups(List.of(
                            ProductOptionGroup.builder()
                                    .groupName("색상")
                                    .values(List.of(
                                            ProductOptionValue.builder().value("화이트").build()
                                    ))
                                    .build()
                    ))
                    .skus(List.of(
                            Sku.builder()
                                    .priceAdjustment(0)
                                    .stockQuantity(10L)
                                    .optionValueIndexes(List.of(5L)) // 잘못된 인덱스
                                    .build()
                    ))
                    .build();

            // when / then
            assertThatThrownBy(() -> mapper.toEntity(domain))
                    .isInstanceOf(ProductException.class)
                    .hasMessageContaining(ErrorCode.OUT_OF_OPTION_INDEX.getMessage());
        }
    }
}