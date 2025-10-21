package com.homesweet.homesweetback.domain.product.category.service.impl;

import com.homesweet.homesweetback.domain.product.category.service.ProductCategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("제품 카테고리 서비스 단위 테스트")
class ProductCategoryServiceImplTest {

    @InjectMocks
    private ProductCategoryService productCategoryService;

    @Nested
    @DisplayName("카테고리 생성 테스트")
    class CreateCategory {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("최상위 카테고리(1레벨) 생성할 수 있다")
            void createTopLevelCategory() {

            }

            @Test
            @DisplayName("2레벨 카테고리 생성할 수 있다")
            void createSecondLevelCategory() {

            }

            @Test
            @DisplayName("3레벨 카테고리 생성할 수 있다")
            void createThirdLevelCategory() {

            }

            @Test
            @DisplayName("같은 부모 카테고리를 갖는 여러 하위 카테고리를 생성할 수 있다")
            void createChildCategoryWithSameParent() {

            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Fail {

            @Test
            @DisplayName("3레벨 넘어서는 카테고리 생성 시 CategoryDepthExceededException이 발생한다")
            void createCategoryWithDepthOver() {

            }

            @Test
            @DisplayName("카테고리 이름은 중복되어 생성 시 DuplicatedCategoryNameException이 발생한다")
            void createCategoryWithDuplicateName() {

            }

            @Test
            @DisplayName("존재하지 않는 부모 ID로 하위 카테고리를 생성할 수 없다")
            void createChildCategoryWithNotExistParent() {

            }
        }
    }
}