package com.homesweet.homesweetback.domain.product.category.repository.jpa.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;
import static org.assertj.core.api.Assertions.assertThat;


/**
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 10.
 */
class ProductCategoryEntityTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("엔티티 생성")
    class CreateEntity {

        @Test
        @DisplayName("빌더로 카테고리를 생성할 수 있다")
        void createCategory_success() {
            // given
            ProductCategoryEntity category = ProductCategoryEntity.builder()
                    .id(1L)
                    .name("가구")
                    .parentId(null)
                    .depth(0)
                    .build();

            // then
            assertThat(category.getId()).isEqualTo(1L);
            assertThat(category.getName()).isEqualTo("가구");
            assertThat(category.getParentId()).isNull();
            assertThat(category.getDepth()).isZero();
        }

        @Test
        @DisplayName("깊이가 0~2 범위를 벗어나면 검증에 실패한다")
        void depthValidation_fail() {
            // given
            ProductCategoryEntity invalidCategory = ProductCategoryEntity.builder()
                    .id(2L)
                    .name("잘못된 카테고리")
                    .parentId(1L)
                    .depth(3)
                    .build();

            // when
            Set<ConstraintViolation<ProductCategoryEntity>> violations = validator.validate(invalidCategory);

            // then
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .anyMatch(msg -> msg.contains("카테고리 깊이는 최대 2까지 가능합니다"));
        }

        @Test
        @DisplayName("카테고리 이름이 비어 있으면 예외 발생")
        void invalidName_blank() {
            ProductCategoryEntity category = ProductCategoryEntity.builder()
                    .id(1L)
                    .name(" ")
                    .parentId(null)
                    .depth(0)
                    .build();

            Set<ConstraintViolation<ProductCategoryEntity>> violations = validator.validate(category);

            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("카테고리 이름은 필수입니다.");
        }
    }
}