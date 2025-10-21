package com.homesweet.homesweetback.domain.product.category.service.impl;

import com.homesweet.homesweetback.domain.product.category.controller.request.CategoryCreateRequest;
import com.homesweet.homesweetback.domain.product.category.controller.response.CategoryResponse;
import com.homesweet.homesweetback.domain.product.category.domain.ProductCategory;
import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.category.domain.exception.ProductCategoryException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("제품 카테고리 서비스 단위 테스트")
class ProductCategoryServiceImplTest {

    @InjectMocks
    private ProductCategoryServiceImpl service;

    @Mock
    private ProductCategoryRepository repository;

    @Nested
    @DisplayName("카테고리 생성")
    class CreateCategory {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("최상위 카테고리(1레벨) 생성 성공")
            void createTopLevelCategory() {

                CategoryCreateRequest request = CategoryCreateRequest.builder()
                        .name("가구")
                        .parentId(null)
                        .build();

                ProductCategory savedCategory = ProductCategory.builder()
                        .id(1L)
                        .name("가구")
                        .parentId(null)
                        .depth(0)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                given(repository.findByName(request.name())).willReturn(Optional.empty());
                given(repository.save(any(ProductCategory.class))).willReturn(savedCategory);

                CategoryResponse response = service.createCategory(request);

                assertThat(response.id()).isEqualTo(1L);
                assertThat(response.name()).isEqualTo("가구");
                assertThat(response.parentId()).isNull();

            }

            @Test
            @DisplayName("같은 부모 카테고리를 갖는 여러 하위 카테고리를 생성할 수 있다")
            void createChildCategoryWithSameParent() {
                // given
                ProductCategory parentCategory = ProductCategory.builder()
                        .id(1L)
                        .name("가구")
                        .parentId(null)
                        .depth(0)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                // 첫 번째 하위 카테고리 생성
                CategoryCreateRequest firstRequest = CategoryCreateRequest.builder()
                        .name("침대")
                        .parentId(1L)
                        .build();

                ProductCategory firstSavedCategory = ProductCategory.builder()
                        .id(2L)
                        .name("침대")
                        .parentId(1L)
                        .depth(1)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                given(repository.findByName("침대")).willReturn(Optional.empty());
                given(repository.findById(1L)).willReturn(Optional.of(parentCategory));
                given(repository.save(any(ProductCategory.class))).willReturn(firstSavedCategory);

                // when
                CategoryResponse firstResponse = service.createCategory(firstRequest);

                // then
                assertThat(firstResponse.id()).isEqualTo(2L);
                assertThat(firstResponse.name()).isEqualTo("침대");
                assertThat(firstResponse.parentId()).isEqualTo(1L);

                // 두 번째 하위 카테고리 생성
                CategoryCreateRequest secondRequest = CategoryCreateRequest.builder()
                        .name("소파")
                        .parentId(1L)
                        .build();

                ProductCategory secondSavedCategory = ProductCategory.builder()
                        .id(3L)
                        .name("소파")
                        .parentId(1L)
                        .depth(1)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                given(repository.findByName("소파")).willReturn(Optional.empty());
                given(repository.save(any(ProductCategory.class))).willReturn(secondSavedCategory);

                // when
                CategoryResponse secondResponse = service.createCategory(secondRequest);

                // then
                assertThat(secondResponse.id()).isEqualTo(3L);
                assertThat(secondResponse.name()).isEqualTo("소파");
                assertThat(secondResponse.parentId()).isEqualTo(1L);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Fail {

            @Test
            @DisplayName("3레벨 넘어서는 카테고리 생성 시 ProductCategoryException이 발생한다")
            void createCategoryWithDepthOver() {
                // given
                CategoryCreateRequest request = CategoryCreateRequest.builder()
                        .name("더블 침대")
                        .parentId(3L)
                        .build();

                ProductCategory parentCategory = ProductCategory.builder()
                        .id(3L)
                        .name("싱글 침대")
                        .parentId(2L)
                        .depth(2)  // 이미 MAX_DEPTH
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                given(repository.findByName(request.name())).willReturn(Optional.empty());
                given(repository.findById(3L)).willReturn(Optional.of(parentCategory));

                // when & then
                assertThatThrownBy(() -> service.createCategory(request))
                        .isInstanceOf(ProductCategoryException.class)
                        .hasMessageContaining(ErrorCode.CATEGORY_DEPTH_EXCEEDED_ERROR.getMessage());
            }

            @Test
            @DisplayName("카테고리 이름이 중복되면 ProductCategoryException이 발생한다")
            void createCategoryWithDuplicateName() {
                // given
                CategoryCreateRequest request = CategoryCreateRequest.builder()
                        .name("가구")
                        .parentId(null)
                        .build();

                ProductCategory existingCategory = ProductCategory.builder()
                        .id(1L)
                        .name("가구")
                        .parentId(null)
                        .depth(0)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                given(repository.findByName(request.name())).willReturn(Optional.of(existingCategory));

                // when & then
                assertThatThrownBy(() -> service.createCategory(request))
                        .isInstanceOf(ProductCategoryException.class)
                        .hasMessageContaining(ErrorCode.DUPLICATED_CATEGORY_NAME_ERROR.getMessage());
            }

            @Test
            @DisplayName("존재하지 않는 부모 ID로 하위 카테고리를 생성할 수 없다")
            void createChildCategoryWithNotExistParent() {
                // given
                CategoryCreateRequest request = CategoryCreateRequest.builder()
                        .name("침대")
                        .parentId(999L)  // 존재하지 않는 부모 ID
                        .build();

                given(repository.findByName(request.name())).willReturn(Optional.empty());
                given(repository.findById(999L)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> service.createCategory(request))
                        .isInstanceOf(ProductCategoryException.class)
                        .hasMessageContaining(ErrorCode.CANNOT_FOUND_PARENT_CATEGORY_ERROR.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("카테고리 조회")
    class FindCategory {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("부모 ID를 이용하여 하위 카테고리들을 조회할 수 있다")
            void getCategoriesByParentId() {

                Long parentId = 1L;

                List<ProductCategory> children = List.of(
                        new ProductCategory(2L, "침실가구", 1L, 1, LocalDateTime.now(), LocalDateTime.now()),
                        new ProductCategory(3L, "거실가구", 1L, 1, LocalDateTime.now(), LocalDateTime.now()),
                        new ProductCategory(4L, "주방가구", 1L, 1, LocalDateTime.now(), LocalDateTime.now())
                );

                given(repository.findByParentId(parentId)).willReturn(children);

                List<CategoryResponse> responses = service.getCategoriesByParentId(parentId);

                assertThat(responses).hasSize(3);
            }
        }
    }
}