package com.homesweet.homesweetback.domain.product.product.service.impl;

import com.homesweet.homesweetback.domain.product.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("제품 서비스 단위 테스트")
class ProductServiceImplTest {

    @InjectMocks
    private ProductServiceImpl service;

    @Mock
    private ProductRepository repository;

    @Nested
    @DisplayName("상품 생성")
    class CreateProduct {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("단일 옵션으로 상품 등록이 가능하다")
            void createProductWithSingleOption() {

            }

            @Test
            @DisplayName("다중 옵션으로 상품 등록이 가능하다")
            void createProductWithMultipleOption() {

            }

            @Test
            @DisplayName("색상·사이즈 옵션 조합(SKU)마다 서로 다른 재고/가격을 가진 상품을 등록할 수 있다")
            void createProductWithDifferentSku() {

            }

            @Test
            @DisplayName("상품 등록 시 상세 이미지 등록이 가능하다")
            void createProductWithDetailImages() {

            }
        }

        @Nested
        @DisplayName("실패")
        class Fail {

            @Test
            @DisplayName("이미 등록된 상품명으로 등록할 수 없다")
            void createProductWithDuplicateName() {

            }

            @Test
            @DisplayName("상세 사진을 3개를 초과해서 등록할 수 없다")
            void createProductWithDetailImagesOver() {
            }
        }
    }
}