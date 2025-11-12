package com.homesweet.homesweetback.domain.product.product.service.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.util.ScrollResponse;
import com.homesweet.homesweetback.common.valid.ProductValidator;
import com.homesweet.homesweetback.domain.product.category.domain.ProductCategory;
import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.data.CategoryMockData;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.request.create.ProductCreateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.request.update.ProductBasicInfoUpdateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.request.update.ProductImageUpdateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.request.update.ProductSkuUpdateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.request.update.ProductStatusUpdateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.response.*;
import com.homesweet.homesweetback.domain.product.product.domain.Product;
import com.homesweet.homesweetback.domain.product.product.domain.ProductImages;
import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.repository.ProductRepository;
import com.homesweet.homesweetback.domain.product.product.repository.SkuRepository;
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

import static com.homesweet.homesweetback.domain.product.data.CategoryMockData.*;
import static com.homesweet.homesweetback.domain.product.data.ProductMockData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;

/**
 *
 * @author junnukim1007gmail.com
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("상품 서비스 단위 테스트")
class ProductServiceImplTest {

    @InjectMocks
    private ProductServiceImpl service;

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductCategoryRepository categoryRepository;
    @Mock
    private ProductImageUploader productImageUploader;
    @Mock
    private ProductValidator productValidator;
    @Mock
    private SkuRepository skuRepository;


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

                ProductCategory category = createTopCategory(1L, "가구");

                ProductImages uploaded = new ProductImages(
                        "https://s3.aws/main.jpg", List.of()
                );

                given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
                given(productImageUploader.uploadProductImages(any(), any())).willReturn(uploaded);
                given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

                // when
                ProductResponse response = service.registerProduct(sellerId, request, mainImage, List.of());

                // then
                assertThat(response.name()).isEqualTo("무옵션 상품");
                assertThat(response.imageUrl()).isEqualTo("https://s3.aws/main.jpg");
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

                given(categoryRepository.findById(1L)).willReturn(Optional.of(ProductCategory.builder().id(1L).name("가구").build()));
                given(productImageUploader.uploadProductImages(any(), any())).willReturn(
                        new ProductImages("https://s3.aws/main.jpg", List.of())
                );
                given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

                // when
                ProductResponse response = service.registerProduct(sellerId, request, mainImage, List.of());

                // then
                assertThat(response.name()).isEqualTo("색상 선택 의자");
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

                given(categoryRepository.findById(1L)).willReturn(Optional.of(ProductCategory.builder().id(1L).name("가구").build()));
                given(productImageUploader.uploadProductImages(any(), any())).willReturn(
                        new ProductImages("https://s3.aws/main.jpg", List.of())
                );
                given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

                // when
                ProductResponse response = service.registerProduct(sellerId, request, mainImage, List.of());

                // then
                assertThat(response.name()).isEqualTo("테이블 세트");
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
                        new ProductCreateRequest.SkuRequest(0, 10L, List.of(0, 2)),   // 화이트 + S → 기본가
                        new ProductCreateRequest.SkuRequest(5000, 5L, List.of(1, 3))  // 블랙 + L → +5,000원
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

                given(categoryRepository.findById(any())).willReturn(Optional.of(category));
                given(productImageUploader.uploadProductImages(any(), any())).willReturn(uploaded);
                given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

                // when
                ProductResponse response = service.registerProduct(sellerId, request, mainImage, detailImages);

                // then
                assertThat(response.name()).isEqualTo("상세 이미지 상품");
            }
        }

        @Nested
        @DisplayName("실패")
        class Fail {

        }
    }

    @Nested
    @DisplayName("상품 프리뷰 조회")
    class FindProductReviews {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("상품이 limit 개수보다 많은 경우 다음 페이지 존재 플래그가 true다")
            void getProducts_hasNextPage() {
                // given
                Long cursorId = null;
                Long categoryId = 1L;
                int limit = 2;
                String keyword = null;
                ProductSortType sortType = ProductSortType.LATEST;

                List<ProductPreviewResponse> mockProducts = List.of(
                        createProductPreviewResponse(1L, "의자", "홈스윗", 10000),
                        createProductPreviewResponse(2L, "테이블", "홈스윗", 20000),
                        createProductPreviewResponse(3L, "소파", "홈스윗", 30000)
                );

                given(productRepository.findNextProducts(cursorId, categoryId, limit + 1, keyword, sortType))
                        .willReturn(mockProducts);

                // when
                ScrollResponse<ProductPreviewResponse> response =
                        service.getProductPreview(cursorId, categoryId, limit, keyword, sortType);

                // then
                assertThat(response.contents()).hasSize(limit);
                assertThat(response.hasNext()).isTrue();
                assertThat(response.nextCursorId()).isEqualTo(2L);
            }

            @Test
            @DisplayName("상품이 limit 이하일 경우 hasNext는 false다")
            void getProducts_noNextPage() {
                // given
                Long cursorId = null;
                Long categoryId = 1L;
                int limit = 3;
                List<ProductPreviewResponse> mockProducts = List.of(
                        createProductPreviewResponse(1L, "의자", "홈스윗", 10000),
                        createProductPreviewResponse(2L, "테이블", "홈스윗", 20000)
                );

                given(productRepository.findNextProducts(cursorId, categoryId, limit + 1, null, ProductSortType.LATEST))
                        .willReturn(mockProducts);

                // when
                ScrollResponse<ProductPreviewResponse> response =
                        service.getProductPreview(cursorId, categoryId, limit, null, ProductSortType.LATEST);

                // then
                assertThat(response.hasNext()).isFalse();
                assertThat(response.nextCursorId()).isNull();
                assertThat(response.contents()).hasSize(2);
            }

            @Test
            @DisplayName("키워드 검색과 정렬 타입이 함께 적용되어도 정상적으로 페이지네이션 된다")
            void getProducts_withKeywordAndSortType() {
                // given
                Long cursorId = null;
                Long categoryId = null;
                int limit = 2;
                String keyword = "테이블";
                ProductSortType sortType = ProductSortType.POPULAR;

                List<ProductPreviewResponse> mockProducts = List.of(
                        createProductPreviewResponse(1L, "의자", "홈스윗", 10000)
                );

                given(productRepository.findNextProducts(cursorId, categoryId, limit + 1, keyword, sortType))
                        .willReturn(mockProducts);

                // when
                ScrollResponse<ProductPreviewResponse> response =
                        service.getProductPreview(cursorId, categoryId, limit, keyword, sortType);

                // then
                assertThat(response.contents()).hasSize(1);
                assertThat(response.hasNext()).isFalse();
            }

        }

        @Nested
        @DisplayName("실패")
        class Fail {

        }
    }

    @Nested
    @DisplayName("상품 상세 조회")
    class FindProductDetail {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("유효한 상품 ID로 상세 정보를 조회할 수 있다")
            void getProductDetail_success() {
                // given
                Long productId = 1L;
                ProductDetailResponse mockResponse = createMockDetailResponse(productId, "테이블", "홈스윗");

                willDoNothing().given(productValidator).validateExistsProduct(productId);
                given(productRepository.findProductDetailById(productId)).willReturn(mockResponse);

                // when
                ProductDetailResponse response = service.getProductDetail(productId);

                // then
                assertThat(response.id()).isEqualTo(productId);
                assertThat(response.name()).isEqualTo("테이블");
                assertThat(response.brand()).isEqualTo("홈스윗");
                assertThat(response.detailImageUrls()).hasSize(2);
                assertThat(response.discountedPrice()).isEqualTo(90000);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Fail {

            @Test
            @DisplayName("상품이 존재하지 않으면 ProductException이 발생한다")
            void getProductDetail_notFound() {
                // given
                Long invalidProductId = 999L;
                willThrow(new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR))
                        .given(productValidator).validateExistsProduct(invalidProductId);

                // when & then
                assertThatThrownBy(() -> service.getProductDetail(invalidProductId))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.PRODUCT_NOT_FOUND_ERROR.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("상품 제고 정보 조회")
    class FindProductSkuInfo {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("유효한 상품 ID로 SKU 재고 목록을 조회할 수 있다")
            void getProductStock_success() {
                // given
                Long productId = 1L;
                List<SkuStockResponse> mockSkus = List.of(
                        createMockSku(101L, 10L, 0, "색상", "화이트", "사이즈", "S"),
                        createMockSku(102L, 5L, 3000, "색상", "블랙", "사이즈", "L")
                );

                willDoNothing().given(productValidator).validateExistsProduct(productId);
                given(productRepository.findSkuStocksByProductId(productId)).willReturn(mockSkus);

                // when
                List<SkuStockResponse> result = service.getProductStock(productId);

                // then
                assertThat(result).hasSize(2);
                assertThat(result.getFirst().skuId()).isEqualTo(101L);
                assertThat(result.getFirst().stockQuantity()).isEqualTo(10L);
                assertThat(result.getFirst().priceAdjustment()).isEqualTo(0);
                assertThat(result.getFirst().options()).hasSize(2);
                assertThat(result.getFirst().options().get(0).groupName()).isEqualTo("색상");
                assertThat(result.getFirst().options().get(1).valueName()).isEqualTo("S");
            }

            @Test
            @DisplayName("SKU가 없는 상품은 빈 리스트를 반환한다")
            void getProductStock_empty() {
                // given
                Long productId = 2L;
                willDoNothing().given(productValidator).validateExistsProduct(productId);
                given(productRepository.findSkuStocksByProductId(productId)).willReturn(List.of());

                // when
                List<SkuStockResponse> result = service.getProductStock(productId);

                // then
                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Fail {

            @Test
            @DisplayName("상품이 존재하지 않으면 ProductException이 발생한다")
            void getProductStock_notFound() {
                // given
                Long invalidProductId = 999L;
                willThrow(new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR))
                        .given(productValidator).validateExistsProduct(invalidProductId);

                // when & then
                assertThatThrownBy(() -> service.getProductStock(invalidProductId))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.PRODUCT_NOT_FOUND_ERROR.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("판매자가 등록한 상품 정보 조회")
    class FindSellerProducts {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("판매자가 등록한 상품 목록을 조회할 수 있다")
            void getSellerProducts_success() {
                // given
                Long sellerId = 1L;
                String startDate = "2025-01-01";
                String endDate = "2025-12-31";

                List<ProductManageResponse> mockProducts = List.of(
                        createManageResponse(1L, "패브릭소파", "가구 > 거실가구 > 소파", 250000, new BigDecimal("10.0"), 15L),
                        createManageResponse(2L, "원목식탁", "가구 > 주방가구 > 식탁", 300000, new BigDecimal("5.0"), 8L)
                );

                given(productRepository.findProductsForSeller(sellerId, startDate, endDate))
                        .willReturn(mockProducts);

                // when
                List<ProductManageResponse> result = service.getSellerProducts(sellerId, startDate, endDate);

                // then
                assertThat(result).hasSize(2);
                assertThat(result.get(0).name()).isEqualTo("패브릭소파");
                assertThat(result.get(0).categoryPath()).isEqualTo("가구 > 거실가구 > 소파");
                assertThat(result.get(1).discountRate()).isEqualByComparingTo("5.0");
                assertThat(result.get(0).status()).isEqualTo(ProductStatus.ON_SALE);
            }

            @Test
            @DisplayName("판매자가 등록한 상품이 없는 경우 빈 리스트를 반환한다")
            void getSellerProducts_empty() {
                // given
                Long sellerId = 2L;
                String startDate = "2025-01-01";
                String endDate = "2025-12-31";

                given(productRepository.findProductsForSeller(sellerId, startDate, endDate))
                        .willReturn(List.of());

                // when
                List<ProductManageResponse> result = service.getSellerProducts(sellerId, startDate, endDate);

                // then
                assertThat(result).isEmpty();
            }

            @Test
            @DisplayName("조회 기간이 null인 경우 전체 기간으로 조회해야 한다")
            void getSellerProducts_nullDate() {
                // given
                Long sellerId = 3L;
                String startDate = null;
                String endDate = null;

                List<ProductManageResponse> mockProducts = List.of(
                        createManageResponse(10L, "책상", "가구 > 서재가구 > 책상", 150000, BigDecimal.ZERO, 30L)
                );

                given(productRepository.findProductsForSeller(sellerId, startDate, endDate))
                        .willReturn(mockProducts);

                // when
                List<ProductManageResponse> result = service.getSellerProducts(sellerId, startDate, endDate);

                // then
                assertThat(result).hasSize(1);
                assertThat(result.getFirst().name()).isEqualTo("책상");
                assertThat(result.getFirst().categoryPath()).isEqualTo("가구 > 서재가구 > 책상");
            }
        }
    }

    @Nested
    @DisplayName("상품 기본 업데이트")
    class UpdateProductBasicInfo {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("상품 이름을 변경하지 않고 다른 필드를 수정할 수 있다")
            void updateWithoutNameChange() {
                // given
                Long sellerId = 1L;
                Long productId = 10L;

                Product existing = createMockProduct(productId, sellerId, "원래상품");
                ProductBasicInfoUpdateRequest request = new ProductBasicInfoUpdateRequest(
                        null, // 이름 변경 없음
                        "홈스윗",
                        20000,
                        new BigDecimal("5.0"),
                        "업데이트된 설명",
                        2500
                );

                given(productRepository.findByIdAndSellerId(productId, sellerId))
                        .willReturn(Optional.of(existing));
                willDoNothing().given(productRepository).update(eq(productId), any(Product.class));

                // when
                service.updateBasicInfo(sellerId, productId, request);

                // then
                verify(productRepository).findByIdAndSellerId(productId, sellerId);
                verify(productRepository, never()).existsBySellerIdAndName(anyLong(), anyString());
                verify(productRepository).update(eq(productId), any(Product.class));
            }

            @Test
            @DisplayName("상품 이름을 변경하되 중복이 없는 경우 정상 수정된다")
            void updateWithNameChange_noDuplicate() {
                // given
                Long sellerId = 1L;
                Long productId = 10L;

                Product existing = createMockProduct(productId, sellerId, "기존상품");
                ProductBasicInfoUpdateRequest request = new ProductBasicInfoUpdateRequest(
                        "새상품", // 변경된 이름
                        "홈스윗",
                        20000,
                        new BigDecimal("10.0"),
                        "새로운 설명",
                        3000
                );

                given(productRepository.findByIdAndSellerId(productId, sellerId))
                        .willReturn(Optional.of(existing));
                given(productRepository.existsBySellerIdAndName(sellerId, "새상품"))
                        .willReturn(false);
                willDoNothing().given(productRepository).update(eq(productId), any(Product.class));

                // when
                service.updateBasicInfo(sellerId, productId, request);

                // then
                verify(productRepository).existsBySellerIdAndName(sellerId, "새상품");
                verify(productRepository).update(eq(productId), any(Product.class));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Fail {

            @Test
            @DisplayName("상품이 존재하지 않으면 ProductException이 발생한다")
            void updateProduct_notFound() {
                // given
                Long sellerId = 1L;
                Long productId = 999L;
                ProductBasicInfoUpdateRequest request = new ProductBasicInfoUpdateRequest(
                        "업데이트상품", "홈스윗", 20000, new BigDecimal("5.0"), "설명", 3000
                );

                given(productRepository.findByIdAndSellerId(productId, sellerId))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> service.updateBasicInfo(sellerId, productId, request))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.PRODUCT_NOT_FOUND_ERROR.getMessage());
            }

            @Test
            @DisplayName("변경하려는 상품 이름이 중복되면 ProductException이 발생한다")
            void updateProduct_duplicateName() {
                // given
                Long sellerId = 1L;
                Long productId = 10L;
                Product existing = createMockProduct(productId, sellerId, "기존상품");

                ProductBasicInfoUpdateRequest request = new ProductBasicInfoUpdateRequest(
                        "중복상품", // 중복 이름
                        "홈스윗",
                        15000,
                        new BigDecimal("3.0"),
                        "변경된 설명",
                        2500
                );

                given(productRepository.findByIdAndSellerId(productId, sellerId))
                        .willReturn(Optional.of(existing));
                given(productRepository.existsBySellerIdAndName(sellerId, "중복상품"))
                        .willReturn(true);

                // when & then
                assertThatThrownBy(() -> service.updateBasicInfo(sellerId, productId, request))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.DUPLICATED_PRODUCT_NAME_ERROR.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("상품 재고 정보 업데이트")
    class UpdateProductStock {
        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("판매자가 실제 소유한 상품의 SKU 재고를 모두 수정할 수 있다")
            void updateSkuStock_success() {
                // given
                Long sellerId = 1L;
                Long productId = 10L;
                ProductSkuUpdateRequest request = createSkuUpdateRequest();

                willDoNothing().given(productValidator).validateExistsSellerProduct(sellerId, productId);
                given(skuRepository.findById(1L)).willReturn(Optional.of(createMockSku(1L)));
                given(skuRepository.findById(2L)).willReturn(Optional.of(createMockSku(2L)));
                willDoNothing().given(skuRepository).updateSku(anyLong(), anyLong(), any());

                // when
                service.updateSkuStock(sellerId, productId, request);

                // then
                verify(productValidator).validateExistsSellerProduct(sellerId, productId);
                verify(skuRepository, times(2)).findById(anyLong());
                verify(skuRepository, times(2)).updateSku(anyLong(), anyLong(), any());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Fail {

            @Test
            @DisplayName("판매자가 소유하지 않은 상품이면 ProductException 발생")
            void updateSkuStock_invalidSeller() {
                // given
                Long sellerId = 99L;
                Long productId = 10L;
                ProductSkuUpdateRequest request = createSkuUpdateRequest();

                willThrow(new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR))
                        .given(productValidator).validateExistsSellerProduct(sellerId, productId);

                // when & then
                assertThatThrownBy(() -> service.updateSkuStock(sellerId, productId, request))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.PRODUCT_NOT_FOUND_ERROR.getMessage());
            }

            @Test
            @DisplayName("SKU가 존재하지 않으면 ProductException 발생 (이미 처리된 SKU는 업데이트됨)")
            void updateSkuStock_skuNotFound() {
                // given
                Long sellerId = 1L;
                Long productId = 10L;
                ProductSkuUpdateRequest request = createSkuUpdateRequest();

                willDoNothing().given(productValidator).validateExistsSellerProduct(sellerId, productId);
                given(skuRepository.findById(1L)).willReturn(Optional.of(createMockSku(1L)));
                given(skuRepository.findById(2L)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> service.updateSkuStock(sellerId, productId, request))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.SKU_NOT_FOUND_ERROR.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("상품 상태 정보 업데이트")
    class UpdateProductStatus {
        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("판매자가 자신의 상품 상태를 정상적으로 변경할 수 있다")
            void updateProductStatus_success() {
                // given
                Long sellerId = 1L;
                Long productId = 100L;
                ProductStatusUpdateRequest request = new ProductStatusUpdateRequest(ProductStatus.OUT_OF_STOCK);

                Product existing = createMockProduct(productId, sellerId, ProductStatus.ON_SALE);

                given(productRepository.findByIdAndSellerId(productId, sellerId))
                        .willReturn(Optional.of(existing));
                willDoNothing().given(productRepository).updateStatus(eq(productId), eq(ProductStatus.OUT_OF_STOCK));

                // when
                service.updateProductStatus(sellerId, productId, request);

                // then
                verify(productRepository).findByIdAndSellerId(productId, sellerId);
                verify(productRepository).updateStatus(productId, ProductStatus.OUT_OF_STOCK);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Fail {

            @Test
            @DisplayName("판매자가 등록하지 않은 상품이면 ProductException 발생")
            void updateProductStatus_notFound() {
                // given
                Long sellerId = 1L;
                Long productId = 999L;
                ProductStatusUpdateRequest request = new ProductStatusUpdateRequest(ProductStatus.SUSPENDED);

                given(productRepository.findByIdAndSellerId(productId, sellerId))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> service.updateProductStatus(sellerId, productId, request))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.PRODUCT_NOT_FOUND_ERROR.getMessage());

                verify(productRepository).findByIdAndSellerId(productId, sellerId);
                verify(productRepository, never()).updateStatus(anyLong(), any());
            }
        }
    }

    @Nested
    @DisplayName("상품 이미지 업데이트")
    class UpdateProductImage {
        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("대표 이미지를 교체할 수 있다")
            void updateMainImage_success() {
                // given
                Long sellerId = 1L;
                Long productId = 100L;
                Product product = createMockProductWithImage(productId, sellerId, "https://s3.aws/old_main.jpg");

                MultipartFile newMain = createMockFile("new_main");
                ProductImageUpdateRequest request = new ProductImageUpdateRequest(newMain, List.of(), List.of());

                given(productRepository.findByIdAndSellerId(productId, sellerId)).willReturn(Optional.of(product));
                willDoNothing().given(productImageUploader).deleteImage(anyString());
                given(productImageUploader.uploadProductMainImage(newMain)).willReturn("https://s3.aws/new_main.jpg");
                willDoNothing().given(productRepository).updateMainImage(productId, "https://s3.aws/new_main.jpg");

                // when
                service.updateImages(sellerId, productId, request);

                // then
                verify(productImageUploader).deleteImage("https://s3.aws/old_main.jpg");
                verify(productImageUploader).uploadProductMainImage(newMain);
                verify(productRepository).updateMainImage(productId, "https://s3.aws/new_main.jpg");
                verifyNoMoreInteractions(productImageUploader, productRepository);
            }

            @Test
            @DisplayName("상세 이미지를 삭제할 수 있다")
            void deleteDetailImages_success() {
                // given
                Long sellerId = 1L;
                Long productId = 101L;
                Product product = createMockProductWithImage(productId, sellerId, "https://s3.aws/main.jpg");

                List<String> deleteTargets = List.of(
                        "https://s3.aws/detail1.jpg",
                        "https://s3.aws/detail2.jpg"
                );

                ProductImageUpdateRequest request = new ProductImageUpdateRequest(null, List.of(), deleteTargets);

                given(productRepository.findByIdAndSellerId(productId, sellerId)).willReturn(Optional.of(product));
                willDoNothing().given(productImageUploader).deleteImage(anyString());
                willDoNothing().given(productRepository).deleteDetailImages(productId, deleteTargets);

                // when
                service.updateImages(sellerId, productId, request);

                // then
                verify(productImageUploader, times(2)).deleteImage(anyString());
                verify(productRepository).deleteDetailImages(productId, deleteTargets);
            }

            @Test
            @DisplayName("상세 이미지를 추가할 수 있다 (5개 이하 제한 검증 포함)")
            void addDetailImages_success() {
                // given
                Long sellerId = 1L;
                Long productId = 102L;
                Product product = createMockProductWithImage(productId, sellerId, "https://s3.aws/main.jpg");

                List<MultipartFile> newDetails = List.of(
                        createMockFile("detail1"),
                        createMockFile("detail2")
                );

                ProductImageUpdateRequest request = new ProductImageUpdateRequest(null, newDetails, List.of());

                given(productRepository.findByIdAndSellerId(productId, sellerId)).willReturn(Optional.of(product));
                willDoNothing().given(productValidator)
                        .validateDetailImageLimit(eq(product), anyList(), eq(newDetails));

                List<String> uploadedUrls = List.of("https://s3.aws/detail1.jpg", "https://s3.aws/detail2.jpg");
                given(productImageUploader.uploadProductDetailImages(newDetails)).willReturn(uploadedUrls);
                willDoNothing().given(productRepository).addDetailImages(productId, uploadedUrls);

                // when
                service.updateImages(sellerId, productId, request);

                // then
                verify(productValidator).validateDetailImageLimit(product, List.of(), newDetails);
                verify(productImageUploader).uploadProductDetailImages(newDetails);
                verify(productRepository).addDetailImages(productId, uploadedUrls);
            }

            @Test
            @DisplayName("대표, 상세 삭제, 상세 추가가 모두 함께 수행될 수 있다")
            void fullUpdate_success() {
                // given
                Long sellerId = 1L;
                Long productId = 103L;
                Product product = createMockProductWithImage(productId, sellerId, "https://s3.aws/old_main.jpg");

                MultipartFile newMain = createMockFile("new_main");
                List<MultipartFile> newDetails = List.of(createMockFile("detail1"));
                List<String> deleteTargets = List.of("https://s3.aws/old_detail.jpg");

                ProductImageUpdateRequest request =
                        new ProductImageUpdateRequest(newMain, newDetails, deleteTargets);

                given(productRepository.findByIdAndSellerId(productId, sellerId)).willReturn(Optional.of(product));
                willDoNothing().given(productImageUploader).deleteImage(anyString());
                given(productImageUploader.uploadProductMainImage(newMain))
                        .willReturn("https://s3.aws/new_main.jpg");
                given(productImageUploader.uploadProductDetailImages(newDetails))
                        .willReturn(List.of("https://s3.aws/detail1.jpg"));

                willDoNothing().given(productValidator)
                        .validateDetailImageLimit(eq(product), eq(deleteTargets), eq(newDetails));

                // when
                service.updateImages(sellerId, productId, request);

                // then
                verify(productImageUploader).deleteImage("https://s3.aws/old_main.jpg");
                verify(productImageUploader).uploadProductMainImage(newMain);
                verify(productImageUploader).deleteImage("https://s3.aws/old_detail.jpg");
                verify(productValidator).validateDetailImageLimit(product, deleteTargets, newDetails);
                verify(productImageUploader).uploadProductDetailImages(newDetails);
                verify(productRepository).updateMainImage(productId, "https://s3.aws/new_main.jpg");
                verify(productRepository).deleteDetailImages(productId, deleteTargets);
                verify(productRepository).addDetailImages(productId, List.of("https://s3.aws/detail1.jpg"));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Fail {

            @Test
            @DisplayName("상품이 존재하지 않으면 ProductException 발생")
            void updateImages_notFound() {
                // given
                Long sellerId = 1L;
                Long productId = 999L;
                ProductImageUpdateRequest request =
                        new ProductImageUpdateRequest(null, List.of(), List.of());

                given(productRepository.findByIdAndSellerId(productId, sellerId))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> service.updateImages(sellerId, productId, request))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.PRODUCT_NOT_FOUND_ERROR.getMessage());
            }
        }
    }
}