package com.homesweet.homesweetback.domain.product.category.repository.impl;


import com.homesweet.homesweetback.domain.product.category.domain.ProductCategory;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.ProductCategoryJPARepository;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.category.repository.mapper.ProductCategoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.homesweet.homesweetback.domain.product.data.CategoryMockData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 10.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductCategoryRepositoryImpl 단위 테스트")
class ProductCategoryRepositoryImplTest {

    @InjectMocks
    private ProductCategoryRepositoryImpl repository;

    @Mock
    private ProductCategoryJPARepository jpaRepository;

    @Mock
    private ProductCategoryMapper mapper;

    @Nested
    @DisplayName("카테고리 저장")
    class SaveCategory {

        @Test
        @DisplayName("카테고리를 저장하면 매퍼 변환과 save가 정상 수행된다")
        void saveCategory_success() {
            // given
            Long categoryId = 1L;
            String name = "가구";
            ProductCategory domain = createTopCategory(categoryId, name);

            ProductCategoryEntity entity = createCategoryEntity(categoryId, name, null, 0);

            given(mapper.toEntity(domain)).willReturn(entity);
            given(jpaRepository.save(entity)).willReturn(entity);
            given(mapper.toDomain(entity)).willReturn(domain);

            // when
            ProductCategory result = repository.save(domain);

            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("가구");
        }
    }

    @Nested
    @DisplayName("카테고리 단일 조회")
    class FindById {

        @Test
        @DisplayName("ID로 카테고리를 조회할 수 있다")
        void findById_success() {
            // given
            ProductCategoryEntity entity = createCategoryEntity(1L, "가구", null, 0);
            ProductCategory domain = createTopCategory(1L, "가구");

            given(jpaRepository.findById(1L)).willReturn(Optional.of(entity));
            given(mapper.toDomain(entity)).willReturn(domain);

            // when
            Optional<ProductCategory> result = repository.findById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("가구");
            assertThat(result.get().depth()).isZero();
        }

        @Test
        @DisplayName("ID로 조회 결과가 없으면 Optional.empty()를 반환한다")
        void findById_notFound() {
            // given
            given(jpaRepository.findById(anyLong())).willReturn(Optional.empty());

            // when
            Optional<ProductCategory> result = repository.findById(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("카테고리 이름으로 조회")
    class FindByName {

        @Test
        @DisplayName("이름으로 카테고리를 조회할 수 있다")
        void findByName_success() {
            ProductCategoryEntity entity = createCategoryEntity(1L, "가구", null, 0);
            ProductCategory domain = createTopCategory(1L, "가구");

            given(jpaRepository.findByName("가구")).willReturn(Optional.of(entity));
            given(mapper.toDomain(entity)).willReturn(domain);

            Optional<ProductCategory> result = repository.findByName("가구");

            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("가구");
        }

        @Test
        @DisplayName("이름이 존재하지 않으면 Optional.empty() 반환")
        void findByName_notFound() {
            given(jpaRepository.findByName("없는카테고리")).willReturn(Optional.empty());

            Optional<ProductCategory> result = repository.findByName("없는카테고리");

        }
    }

    @Nested
    @DisplayName("부모 ID로 하위 카테고리 조회")
    class FindByParentId {

        @Test
        @DisplayName("부모 ID로 하위 카테고리 목록을 조회할 수 있다")
        void findByParentId_success() {
            List<ProductCategoryEntity> entities = List.of(
                    createCategoryEntity(2L, "의자", 1L, 1),
                    createCategoryEntity(3L, "책상", 1L, 1)
            );

            List<ProductCategory> domains = List.of(
                    createMidCategory(2L, "의자", 1L),
                    createMidCategory(3L, "책상", 1L)
            );

            given(jpaRepository.findByParentId(1L)).willReturn(entities);
            given(mapper.toDomain(entities.get(0))).willReturn(domains.get(0));
            given(mapper.toDomain(entities.get(1))).willReturn(domains.get(1));

            List<ProductCategory> result = repository.findByParentId(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("의자");
            assertThat(result.get(1).name()).isEqualTo("책상");
        }

        @Test
        @DisplayName("하위 카테고리가 없으면 빈 리스트 반환")
        void findByParentId_empty() {
            given(jpaRepository.findByParentId(999L)).willReturn(List.of());

            List<ProductCategory> result = repository.findByParentId(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("최상위 카테고리 조회")
    class FindTopLevelCategories {

        @Test
        @DisplayName("parentId가 null인 최상위 카테고리를 조회할 수 있다")
        void findTopLevelCategories_success() {
            List<ProductCategoryEntity> entities = List.of(
                    createCategoryEntity(1L, "가구", null, 0),
                    createCategoryEntity(10L, "가전제품", null, 0)
            );

            List<ProductCategory> domains = List.of(
                    createTopCategory(1L, "가구"),
                    createTopCategory(10L, "가전제품")
            );

            given(jpaRepository.findByParentIdIsNull()).willReturn(entities);
            given(mapper.toDomain(entities.get(0))).willReturn(domains.get(0));
            given(mapper.toDomain(entities.get(1))).willReturn(domains.get(1));

            List<ProductCategory> result = repository.findTopLevelCategories();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("가구");
            assertThat(result.get(1).name()).isEqualTo("가전제품");
        }

        @Test
        @DisplayName("최상위 카테고리가 없으면 빈 리스트 반환")
        void findTopLevelCategories_empty() {
            given(jpaRepository.findByParentIdIsNull()).willReturn(List.of());

            List<ProductCategory> result = repository.findTopLevelCategories();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("모든 하위 카테고리 ID 조회")
    class FindAllSubCategoryIds {

        @Test
        @DisplayName("카테고리 ID를 기준으로 하위 ID 목록을 반환한다")
        void findAllSubCategoryIds_success() {
            List<Long> subCategoryIds = List.of(2L, 3L, 4L);
            given(jpaRepository.findAllSubCategoryIds(1L)).willReturn(subCategoryIds);

            List<Long> result = repository.findAllSubCategoryIds(1L);

            assertThat(result).containsExactly(2L, 3L, 4L);
        }

        @Test
        @DisplayName("하위 카테고리가 없으면 빈 리스트 반환")
        void findAllSubCategoryIds_empty() {
            given(jpaRepository.findAllSubCategoryIds(999L)).willReturn(List.of());

            List<Long> result = repository.findAllSubCategoryIds(999L);

            assertThat(result).isEmpty();
        }
    }
}