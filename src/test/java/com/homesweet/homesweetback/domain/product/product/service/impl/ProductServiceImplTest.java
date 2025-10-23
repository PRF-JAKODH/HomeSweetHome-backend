package com.homesweet.homesweetback.domain.product.product.service.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.product.category.domain.ProductCategory;
import com.homesweet.homesweetback.domain.product.category.domain.exception.ProductCategoryException;
import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductCreateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductResponse;
import com.homesweet.homesweetback.domain.product.product.domain.Product;
import com.homesweet.homesweetback.domain.product.product.domain.ProductImages;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.repository.ProductRepository;
import com.homesweet.homesweetback.domain.product.product.repository.util.ProductImageUploader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
    private ProductRepository productRepository;
    @Mock
    private ProductCategoryRepository categoryRepository;
    @Mock
    private ProductImageUploader productImageUploader;

    @Nested
    @DisplayName("상품 생성")
    class CreateProduct {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("옵션이 없는 상품을 등록할 수 있다")
            void createProductWithoutOption() {
                // given
                Long sellerId = 1L;

                ProductCreateRequest request = new ProductCreateRequest(
                        1L, "무옵션 상품", "홈스윗",
                        10000, BigDecimal.ZERO,
                        "단일 구성 상품", 3000,
                        List.of(), List.of()
                );

                MockMultipartFile mainImage =
                        new MockMultipartFile("mainImage", "main.jpg", "image/jpeg", "data".getBytes());

                ProductCategory category = ProductCategory.builder()
                        .id(1L)
                        .name("가구")
                        .build();

                ProductImages uploaded = new ProductImages(
                        "https://s3.aws/main.jpg", List.of()
                );

                given(productRepository.existsBySellerIdAndName(any(), any())).willReturn(false);
                given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
                given(productImageUploader.uploadProductImages(any(), any())).willReturn(uploaded);
                given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

                // when
                ProductResponse response = service.registerProduct(sellerId, request, mainImage, List.of());

                // then
                assertThat(response.name()).isEqualTo("무옵션 상품");
                assertThat(response.imageUrl()).isEqualTo("https://s3.aws/main.jpg");
                verify(productRepository).save(any(Product.class));
            }

            @Test
            @DisplayName("단일 옵션 그룹 상품을 등록할 수 있다")
            void createProductWithSingleOption() {
                // given
                Long sellerId = 1L;

                ProductCreateRequest.ProductOptionGroupRequest colorGroup = new ProductCreateRequest.ProductOptionGroupRequest(
                        "색상", List.of("화이트", "내추럴")
                );

                ProductCreateRequest request = new ProductCreateRequest(
                        1L, "색상 선택 의자", "홈스윗",
                        20000, BigDecimal.ZERO,
                        "색상별로 다른 의자", 3000,
                        List.of(colorGroup),
                        List.of()
                );

                MockMultipartFile mainImage =
                        new MockMultipartFile("mainImage", "main.jpg", "image/jpeg", "data".getBytes());

                given(productRepository.existsBySellerIdAndName(any(), any())).willReturn(false);
                given(categoryRepository.findById(1L)).willReturn(Optional.of(ProductCategory.builder().id(1L).name("가구").build()));
                given(productImageUploader.uploadProductImages(any(), any())).willReturn(
                        new ProductImages("https://s3.aws/main.jpg", List.of())
                );
                given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

                // when
                ProductResponse response = service.registerProduct(sellerId, request, mainImage, List.of());

                // then
                assertThat(response.name()).isEqualTo("색상 선택 의자");
                verify(productRepository).save(any(Product.class));
            }

            @Test
            @DisplayName("다중 옵션 그룹 상품을 등록할 수 있다")
            void createProductWithMultipleOptions() {
                // given
                Long sellerId = 1L;

                ProductCreateRequest.ProductOptionGroupRequest color = new ProductCreateRequest.ProductOptionGroupRequest("색상", List.of("화이트", "블랙"));
                ProductCreateRequest.ProductOptionGroupRequest size = new ProductCreateRequest.ProductOptionGroupRequest("사이즈", List.of("S", "L"));

                ProductCreateRequest request = new ProductCreateRequest(
                        1L, "테이블 세트", "홈스윗",
                        150000, new BigDecimal("10.00"),
                        "색상/사이즈 조합 선택 가능", 5000,
                        List.of(color, size),
                        List.of()
                );

                MockMultipartFile mainImage =
                        new MockMultipartFile("mainImage", "main.jpg", "image/jpeg", "data".getBytes());

                given(productRepository.existsBySellerIdAndName(any(), any())).willReturn(false);
                given(categoryRepository.findById(1L)).willReturn(Optional.of(ProductCategory.builder().id(1L).name("가구").build()));
                given(productImageUploader.uploadProductImages(any(), any())).willReturn(
                        new ProductImages("https://s3.aws/main.jpg", List.of())
                );
                given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

                // when
                ProductResponse response = service.registerProduct(sellerId, request, mainImage, List.of());

                // then
                assertThat(response.name()).isEqualTo("테이블 세트");
                verify(productRepository).save(any(Product.class));
            }

            @Test
            @DisplayName("다중 옵션 상품은 옵션 조합별로 서로 다른 재고와 추가 금액을 설정할 수 있다")
            void createProductWithDifferentSkuPerOptionCombination() {
                // given
                Long sellerId = 1L;

                // 옵션 그룹 요청: 색상 + 사이즈
                ProductCreateRequest.ProductOptionGroupRequest color = new ProductCreateRequest.ProductOptionGroupRequest("색상", List.of("화이트", "블랙"));
                ProductCreateRequest.ProductOptionGroupRequest size = new ProductCreateRequest.ProductOptionGroupRequest("사이즈", List.of("S", "L"));

                // SKU 요청: 각 조합별 다른 가격/재고
                List<ProductCreateRequest.SkuRequest> skuRequests = List.of(
                        new ProductCreateRequest.SkuRequest(0, 10, List.of(0, 2)),   // 화이트 + S → 기본가
                        new ProductCreateRequest.SkuRequest(5000, 5, List.of(1, 3))  // 블랙 + L → +5,000원
                );

                ProductCreateRequest request = new ProductCreateRequest(
                        1L, "프리미엄 티셔츠", "홈스윗",
                        30000, new BigDecimal("10.00"),
                        "색상과 사이즈에 따라 다른 가격", 3000,
                        List.of(color, size),
                        skuRequests
                );

                MockMultipartFile mainImage =
                        new MockMultipartFile("mainImage", "main.jpg", "image/jpeg", "data".getBytes());

                ProductCategory category = ProductCategory.builder()
                        .id(1L)
                        .name("의류")
                        .build();

                ProductImages uploaded = new ProductImages(
                        "https://s3.aws/main.jpg",
                        List.of("https://s3.aws/detail1.jpg")
                );

                given(productRepository.existsBySellerIdAndName(any(), any())).willReturn(false);
                given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
                given(productImageUploader.uploadProductImages(any(), any())).willReturn(uploaded);
                given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

                // when
                ProductResponse response = service.registerProduct(sellerId, request, mainImage, List.of());

                // then
                assertThat(response.name()).isEqualTo("프리미엄 티셔츠");
                assertThat(response.skus()).hasSize(2);
                assertThat(response.skus().get(0).priceAdjustment()).isEqualTo(0);
                assertThat(response.skus().get(1).priceAdjustment()).isEqualTo(5000);
                assertThat(response.skus().get(0).stockQuantity()).isEqualTo(10);
                assertThat(response.skus().get(1).stockQuantity()).isEqualTo(5);
                verify(productRepository).save(any(Product.class));
            }

            @Test
            @DisplayName("상세 이미지를 여러 장 등록할 수 있다 (최대 5장)")
            void createProductWithDetailImages() {
                // given
                Long sellerId = 1L;

                ProductCreateRequest request = new ProductCreateRequest(
                        1L, "상세 이미지 상품", "홈스윗",
                        450000, new BigDecimal("5.00"),
                        "디테일이 중요한 상품", 5000,
                        List.of(), List.of()
                );

                MultipartFile mainImage =
                        new MockMultipartFile("mainImage", "main.jpg", "image/jpeg", "data".getBytes());

                List<MultipartFile> detailImages = List.of(
                        new MockMultipartFile("detail1", "detail1.jpg", "image/jpeg", "data".getBytes()),
                        new MockMultipartFile("detail2", "detail2.jpg", "image/jpeg", "data".getBytes()),
                        new MockMultipartFile("detail3", "detail3.jpg", "image/jpeg", "data".getBytes()),
                        new MockMultipartFile("detail4", "detail4.jpg", "image/jpeg", "data".getBytes()),
                        new MockMultipartFile("detail5", "detail5.jpg", "image/jpeg", "data".getBytes())
                );

                ProductCategory category = ProductCategory.builder().id(1L).name("가구").build();

                ProductImages uploaded = new ProductImages(
                        "https://s3.aws/main.jpg",
                        List.of(
                                "https://s3.aws/detail1.jpg",
                                "https://s3.aws/detail2.jpg",
                                "https://s3.aws/detail3.jpg",
                                "https://s3.aws/detail4.jpg",
                                "https://s3.aws/detail5.jpg"
                        )
                );

                given(productRepository.existsBySellerIdAndName(any(), any())).willReturn(false);
                given(categoryRepository.findById(any())).willReturn(Optional.of(category));
                given(productImageUploader.uploadProductImages(any(), any())).willReturn(uploaded);
                given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

                // when
                ProductResponse response = service.registerProduct(sellerId, request, mainImage, detailImages);

                // then
                assertThat(response.name()).isEqualTo("상세 이미지 상품");
                verify(productImageUploader).uploadProductImages(mainImage, detailImages);
                verify(productRepository).save(any(Product.class));
            }
        }

        @Nested
        @DisplayName("실패")
        class Fail {

            @Test
            @DisplayName("이미 등록된 상품명으로 등록할 수 없다")
            void createProductWithDuplicateName() {
                // given
                Long sellerId = 1L;
                ProductCreateRequest request = new ProductCreateRequest(
                        1L, "중복 상품", "홈스윗",
                        20000, BigDecimal.ZERO,
                        "중복 이름 상품", 3000,
                        List.of(), List.of()
                );

                MockMultipartFile mainImage =
                        new MockMultipartFile("mainImage", "main.jpg", "image/jpeg", "data".getBytes());

                given(productRepository.existsBySellerIdAndName(sellerId, "중복 상품")).willReturn(true);

                // when & then
                assertThatThrownBy(() ->
                        service.registerProduct(sellerId, request, mainImage, List.of())
                )
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.DUPLICATED_PRODUCT_NAME_ERROR.getMessage());
            }

            @Test
            @DisplayName("존재하지 않는 카테고리로 등록할 수 없다")
            void createProductWithInvalidCategory() {
                // given
                Long sellerId = 1L;
                ProductCreateRequest request = new ProductCreateRequest(
                        999L, "잘못된 카테고리 상품", "홈스윗",
                        30000, BigDecimal.ZERO,
                        "없는 카테고리", 3000,
                        List.of(), List.of()
                );

                MockMultipartFile mainImage =
                        new MockMultipartFile("mainImage", "main.jpg", "image/jpeg", "data".getBytes());

                given(productRepository.existsBySellerIdAndName(any(), any())).willReturn(false);
                given(categoryRepository.findById(999L)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() ->
                        service.registerProduct(sellerId, request, mainImage, List.of())
                )
                        .isInstanceOf(ProductCategoryException.class)
                        .hasMessage(ErrorCode.CANNOT_FOUND_CATEGORY_ERROR.getMessage());
            }
        }
    }
}