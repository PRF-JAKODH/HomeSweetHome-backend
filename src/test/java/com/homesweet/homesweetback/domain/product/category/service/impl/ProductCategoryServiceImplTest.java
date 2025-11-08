package com.homesweet.homesweetback.domain.product.category.service.impl;

import com.homesweet.homesweetback.common.valid.ProductValidator;
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

import java.util.List;
import java.util.Optional;

import static com.homesweet.homesweetback.domain.product.data.CategoryMockData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
    private ProductValidator validator;

    @Mock
    private ProductCategoryRepository repository;

    @Nested
    @DisplayName("카테고리 생성")
    class CreateCategory {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("최상위 카테고리를 생성할 수 있다 (depth=0)")
            void createTopLevelCategory() {
                // given
                CategoryCreateRequest request = CategoryCreateRequest.builder()
                        .name("가구")
                        .parentId(null)
                        .build();

                ProductCategory saved = createTopCategory(1L, "가구");

                willDoNothing().given(validator).validateDuplicateCategoryName(request.name());
                given(repository.save(any(ProductCategory.class))).willReturn(saved);

                // when
                CategoryResponse response = service.createCategory(request);

                // then
                assertThat(response.name()).isEqualTo("가구");
                assertThat(response.parentId()).isNull();
                assertThat(response.depth()).isEqualTo(0);
            }

            @Test
            @DisplayName("중간 카테고리를 생성할 수 있다 (depth=1)")
            void createMidLevelCategory() {
                // given
                ProductCategory parent = createTopCategory(1L, "가구");

                CategoryCreateRequest request = CategoryCreateRequest.builder()
                        .name("거실가구")
                        .parentId(parent.id())
                        .build();

                ProductCategory saved = createMidCategory(2L, "거실가구", parent.id());

                willDoNothing().given(validator).validateDuplicateCategoryName(request.name());
                given(repository.findById(parent.id())).willReturn(Optional.of(parent));
                given(repository.save(any(ProductCategory.class))).willReturn(saved);

                // when
                CategoryResponse response = service.createCategory(request);

                // then
                assertThat(response.name()).isEqualTo("거실가구");
                assertThat(response.parentId()).isEqualTo(parent.id());
                assertThat(response.depth()).isEqualTo(1);
            }

            @Test
            @DisplayName("하위 카테고리를 생성할 수 있다 (depth=2)")
            void createSubLevelCategory() {
                // given
                ProductCategory parent = createMidCategory(2L, "거실가구", 1L);

                CategoryCreateRequest request = CategoryCreateRequest.builder()
                        .name("소파")
                        .parentId(parent.id())
                        .build();

                ProductCategory saved = createSubCategory(3L, "소파", parent.id());

                willDoNothing().given(validator).validateDuplicateCategoryName(request.name());
                given(repository.findById(parent.id())).willReturn(Optional.of(parent));
                given(repository.save(any(ProductCategory.class))).willReturn(saved);

                // when
                CategoryResponse response = service.createCategory(request);

                // then
                assertThat(response.name()).isEqualTo("소파");
                assertThat(response.depth()).isEqualTo(2);
                assertThat(response.parentId()).isEqualTo(parent.id());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Fail {

            @Test
            @DisplayName("카테고리 최대 깊이를 초과하면 예외가 발생한다 (depth > 2)")
            void exceedMaxDepth() {
                // given
                ProductCategory parent = createSubCategory(3L, "소파", 2L);

                CategoryCreateRequest request = CategoryCreateRequest.builder()
                        .name("더 깊은 카테고리")
                        .parentId(parent.id())
                        .build();

                willDoNothing().given(validator).validateDuplicateCategoryName(request.name());
                given(repository.findById(parent.id())).willReturn(Optional.of(parent));

                // when & then
                assertThatThrownBy(() -> service.createCategory(request))
                        .isInstanceOf(ProductCategoryException.class)
                        .hasMessage(ErrorCode.CATEGORY_DEPTH_EXCEEDED_ERROR.getMessage());
            }

            @Test
            @DisplayName("카테고리 이름이 중복되면 ProductCategoryException이 발생한다")
            void createCategoryWithDuplicateName() {
                // given
                CategoryCreateRequest request = CategoryCreateRequest.builder()
                        .name("가구")
                        .parentId(null)
                        .build();

                willThrow(new ProductCategoryException(ErrorCode.DUPLICATED_CATEGORY_NAME_ERROR))
                        .given(validator).validateDuplicateCategoryName(request.name());

                // when & then
                assertThatThrownBy(() -> service.createCategory(request))
                        .isInstanceOf(ProductCategoryException.class)
                        .hasMessage(ErrorCode.DUPLICATED_CATEGORY_NAME_ERROR.getMessage());
            }

            @Test
            @DisplayName("부모 카테고리를 찾을 수 없으면 예외가 발생한다")
            void parentCategoryNotFound() {
                // given
                CategoryCreateRequest request = CategoryCreateRequest.builder()
                        .name("없는부모")
                        .parentId(999L)
                        .build();

                willDoNothing().given(validator).validateDuplicateCategoryName(request.name());
                given(repository.findById(999L)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> service.createCategory(request))
                        .isInstanceOf(ProductCategoryException.class)
                        .hasMessage(ErrorCode.CANNOT_FOUND_PARENT_CATEGORY_ERROR.getMessage());
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
                        createSubCategory(2L, "침실가구", 1L),
                        createSubCategory(3L, "거실가구", 1L),
                        createSubCategory(4L, "주방가구", 1L)
                );

                given(repository.findByParentId(parentId)).willReturn(children);

                List<CategoryResponse> responses = service.getCategoriesByParentId(parentId);

                assertThat(responses).hasSize(3);
            }

            @Test
            @DisplayName("최상위 카테고리(depth 0) 목록을 조회할 수 있다")
            void getTopLevelCategories() {
                // given
                List<ProductCategory> topLevelCategories = List.of(
                        createTopCategory(1L, "가구"),
                        createTopCategory(5L, "조명"),
                        createTopCategory(10L, "패브릭")
                );

                given(repository.findTopLevelCategories()).willReturn(topLevelCategories);

                // when
                List<CategoryResponse> responses = service.getTopLevelCategories();

                // then
                assertThat(responses).hasSize(3);
                assertThat(responses).extracting("parentId")
                        .containsOnly((Long) null);
                assertThat(responses).extracting("depth")
                        .containsOnly(0);
            }
        }
    }

    @Nested
    @DisplayName("카테고리 계층 전체 조회")
    class FindCategoryHierarchy {
        @Test
        @DisplayName("하위 카테고리로부터 루트까지 계층을 올바르게 조회할 수 있다")
        void getCategoryHierarchy_success() {
            // given
            ProductCategory top = createTopCategory(1L, "가구");
            ProductCategory mid = createMidCategory(2L, "거실가구", top.id());
            ProductCategory sub = createSubCategory(3L, "소파", mid.id());

            // 하위 카테고리로부터 위로 탐색하는 stub
            given(repository.findById(sub.id())).willReturn(Optional.of(sub));
            given(repository.findById(mid.id())).willReturn(Optional.of(mid));
            given(repository.findById(top.id())).willReturn(Optional.of(top));

            // when
            List<CategoryResponse> result = service.getCategoryHierarchy(sub.id());

            // then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).name()).isEqualTo("가구");        // depth 0
            assertThat(result.get(1).name()).isEqualTo("거실가구");   // depth 1
            assertThat(result.get(2).name()).isEqualTo("소파");       // depth 2

            // 부모-자식 관계 검증
            assertThat(result.get(1).parentId()).isEqualTo(result.get(0).id());
            assertThat(result.get(2).parentId()).isEqualTo(result.get(1).id());
        }

        @Test
        @DisplayName("최상위 카테고리는 자기 자신만 반환한다")
        void getCategoryHierarchy_topLevel() {
            // given
            ProductCategory top = createTopCategory(1L, "가전");

            given(repository.findById(top.id())).willReturn(Optional.of(top));

            // when
            List<CategoryResponse> result = service.getCategoryHierarchy(top.id());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("가전");
            assertThat(result.get(0).parentId()).isNull();
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Fail {

        @Test
        @DisplayName("존재하지 않는 카테고리를 조회하면 예외가 발생한다")
        void getCategoryHierarchy_notFound() {
            // given
            Long invalidId = 999L;
            given(repository.findById(invalidId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.getCategoryHierarchy(invalidId))
                    .isInstanceOf(ProductCategoryException.class)
                    .hasMessage(ErrorCode.CANNOT_FOUND_CATEGORY_ERROR.getMessage());
        }
    }
}