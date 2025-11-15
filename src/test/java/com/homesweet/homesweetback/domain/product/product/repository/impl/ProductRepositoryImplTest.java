package com.homesweet.homesweetback.domain.product.product.repository.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductDetailResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductManageResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductPreviewResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.SkuStockResponse;
import com.homesweet.homesweetback.domain.product.product.domain.Product;
import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.ProductJPARepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductDetailImageEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.repository.mapper.ProductMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.homesweet.homesweetback.domain.product.data.ProductMockData.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 8.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("상품 레포지토리 단위 테스트")
class ProductRepositoryImplTest {

    @InjectMocks
    private ProductRepositoryImpl repository;

    @Mock
    private ProductJPARepository jpaRepository;
    @Mock
    private ProductMapper mapper;

    @Nested
    @DisplayName("상품 저장")
    class SaveProduct {
        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("상품을 저장할 수 있다 - 도메인 → 엔티티 → 도메인 변환 검증")
            void save_success() {
                // given
                Product domain = createMockProduct(1L, 100L, "테이블");
                ProductEntity entity = createMockProductEntity(1L, "테이블");

                // stub 변환 로직
                given(mapper.toEntity(domain)).willReturn(entity);
                given(jpaRepository.save(entity)).willReturn(entity);
                given(mapper.toDomain(entity)).willReturn(domain);

                // when
                Product result = repository.save(domain);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getId()).isEqualTo(1L);
                assertThat(result.getName()).isEqualTo("테이블");
            }
        }

        @Nested
        @DisplayName("실패")
        class Fail {
            @Test
            @DisplayName("상품 저장 시 mapper가 null을 반환하면 예외가 발생한다")
            void save_fail_nullMapping() {
                // given
                Product domain = createMockProduct(1L, 100L, "테이블");

                given(mapper.toEntity(domain)).willReturn(null);

                // when & then
                assertThatThrownBy(() -> repository.save(domain))
                        .isInstanceOf(IllegalStateException.class);
            }
        }
    }

    @Nested
    @DisplayName("상품 조회")
    class FindProducts {
        @Nested
        @DisplayName("상품 존재 여부 확인")
        class ExistsById {

            @Test
            @DisplayName("상품이 존재하면 true 반환")
            void existsById_true() {
                given(jpaRepository.existsById(1L)).willReturn(true);

                boolean result = repository.existsById(1L);

                assertThat(result).isTrue();
            }

            @Test
            @DisplayName("상품이 존재하지 않으면 false 반환")
            void existsById_false() {
                given(jpaRepository.existsById(1L)).willReturn(false);

                boolean result = repository.existsById(1L);

                assertThat(result).isFalse();
            }
        }

        @Nested
        @DisplayName("판매자 ID와 상품 ID로 조회")
        class FindByIdAndSellerId {

            @Test
            @DisplayName("상품이 존재하면 Optional.of(Product) 반환")
            void findByIdAndSellerId_success() {
                Long productId = 1L;
                Long sellerId = 100L;

                Product product = createMockProduct(productId, sellerId, "의자");
                ProductEntity entity = createMockProductEntity(productId, "의자");

                given(jpaRepository.findByIdAndSellerId(productId, sellerId))
                        .willReturn(Optional.of(entity));
                given(mapper.toDomain(entity)).willReturn(product);

                Optional<Product> result = repository.findByIdAndSellerId(productId, sellerId);

                assertThat(result).isPresent();
                assertThat(result.get().getName()).isEqualTo("의자");
            }

            @Test
            @DisplayName("상품이 존재하지 않으면 Optional.empty() 반환")
            void findByIdAndSellerId_empty() {
                Long productId = 1L;
                Long sellerId = 100L;

                given(jpaRepository.findByIdAndSellerId(productId, sellerId)).willReturn(Optional.empty());

                Optional<Product> result = repository.findByIdAndSellerId(productId, sellerId);

                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("판매자 ID와 상품 이름으로 중복 조회")
        class ExistsBySellerIdAndName {

            @Test
            @DisplayName("중복 상품이 존재하면 true 반환")
            void existsBySellerIdAndName_true() {
                given(jpaRepository.existsBySellerIdAndName(100L, "의자")).willReturn(true);

                boolean result = repository.existsBySellerIdAndName(100L, "의자");

                assertThat(result).isTrue();
            }

            @Test
            @DisplayName("중복 상품이 없으면 false 반환")
            void existsBySellerIdAndName_false() {
                given(jpaRepository.existsBySellerIdAndName(100L, "의자")).willReturn(false);

                boolean result = repository.existsBySellerIdAndName(100L, "의자");

                assertThat(result).isFalse();
            }
        }

        @Nested
        @DisplayName("무한 스크롤 상품 조회")
        class FindNextProducts {

            @Test
            @DisplayName("상품 리스트를 반환한다")
            void findNextProducts_success() {
                List<ProductEntity> products = List.of(
                        createMockProductEntity(1L, "테이블1"),
                        createMockProductEntity(2L,  "테이블2")
                );

                given(jpaRepository.findNextProducts(any(), any(), anyInt(), any(), any())).willReturn(products);

                List<Product> result =
                        repository.findNextProducts(1L, 1L, 10, "가구", ProductSortType.LATEST);

                assertThat(result).hasSize(2);
            }

            @Test
            @DisplayName("상품이 없으면 빈 리스트 반환")
            void findNextProducts_empty() {
                given(jpaRepository.findNextProducts(any(), any(), anyInt(), any(), any()))
                        .willReturn(Collections.emptyList());

                List<Product> result =
                        repository.findNextProducts(1L, 1L, 10, null, ProductSortType.LATEST);

                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("상품 상세 조회")
        class FindProductDetailById {

            @Test
            @DisplayName("상품 상세 정보 반환")
            void findProductDetailById_success() {
                ProductDetailResponse detail = createMockDetailResponse(1L, "테스트", "브랜드");

                given(jpaRepository.findProductDetailById(1L)).willReturn(Optional.of(detail));

                ProductDetailResponse result = repository.findProductDetailById(1L);

                assertThat(result.name()).isEqualTo("테스트");
            }

            @Test
            @DisplayName("상품 상세 정보가 없으면 예외 발생")
            void findProductDetailById_empty() {
                // given
                given(jpaRepository.findProductDetailById(1L))
                        .willReturn(Optional.empty());

                // then
                assertThatThrownBy(() -> repository.findProductDetailById(1L))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.PRODUCT_NOT_FOUND_ERROR.getMessage());
            }
        }

        @Nested
        @DisplayName("SKU 재고 조회")
        class FindSkuStocksByProductId {

            @Test
            @DisplayName("SKU 재고 목록 반환")
            void findSkuStocksByProductId_success() {
                List<SkuStockResponse> stocks = List.of(
                        createMockSku(101L, 10L, 0, "색상", "화이트", "사이즈", "S")
                );
                given(jpaRepository.findSkuStocksByProductId(1L)).willReturn(stocks);

                List<SkuStockResponse> result = repository.findSkuStocksByProductId(1L);

                assertThat(result).hasSize(1);
                assertThat(result.getFirst().skuId()).isEqualTo(101L);
            }

            @Test
            @DisplayName("재고가 없으면 빈 리스트 반환")
            void findSkuStocksByProductId_empty() {
                given(jpaRepository.findSkuStocksByProductId(1L)).willReturn(Collections.emptyList());

                List<SkuStockResponse> result = repository.findSkuStocksByProductId(1L);

                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("판매자 상품 목록 조회")
        class FindProductsForSeller {

            @Test
            @DisplayName("판매자 상품 리스트 반환")
            void findProductsForSeller_success() {
                List<ProductManageResponse> products = List.of(
                        createManageResponse(1L, "패브릭소파", "가구 > 거실가구 > 소파", 250000, new BigDecimal("10.0"), 15L)
                );
                given(jpaRepository.findProductsForSeller(100L, "2024-01-01", "2024-12-31"))
                        .willReturn(products);

                List<ProductManageResponse> result =
                        repository.findProductsForSeller(100L, "2024-01-01", "2024-12-31");

                assertThat(result).hasSize(1);
                assertThat(result.getFirst().name()).isEqualTo("패브릭소파");
            }

            @Test
            @DisplayName("판매자 상품이 없으면 빈 리스트 반환")
            void findProductsForSeller_empty() {
                given(jpaRepository.findProductsForSeller(anyLong(), any(), any()))
                        .willReturn(Collections.emptyList());

                List<ProductManageResponse> result =
                        repository.findProductsForSeller(100L, "2024-01-01", "2024-12-31");

                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("단일 상품 조회")
        class FindByProductId {

            @Test
            @DisplayName("상품이 존재하면 Product 도메인 객체를 반환한다")
            void findByProductId_success() {
                // given
                Long productId = 1L;
                ProductEntity entity = createMockProductEntity(productId);
                Product domain = createMockProduct(productId, 100L, "의자");

                given(jpaRepository.findById(productId)).willReturn(Optional.of(entity));
                given(mapper.toDomain(entity)).willReturn(domain);

                // when
                Product result = repository.findByProductId(productId);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getName()).isEqualTo("의자");
                verify(mapper).toDomain(entity);
            }

            @Test
            @DisplayName("상품이 존재하지 않으면 ProductException 발생")
            void findByProductId_notFound() {
                // given
                Long productId = 999L;
                given(jpaRepository.findById(productId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> repository.findByProductId(productId))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.PRODUCT_NOT_FOUND_ERROR.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("상품 업데이트")
    class UpdateProduct {
        @Nested
        @DisplayName("상품 상태")
        class UpdateStatus {

            @Test
            @DisplayName("상품이 존재하면 상태가 업데이트된다")
            void updateStatus_success() {
                // given
                Long productId = 1L;
                ProductEntity entity = createMockProductEntity(productId, "의자", ProductStatus.ON_SALE);

                given(jpaRepository.findById(productId)).willReturn(Optional.of(entity));

                // when
                repository.updateStatus(productId, ProductStatus.OUT_OF_STOCK);

                // then
                assertThat(entity.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
            }

            @Test
            @DisplayName("상품이 존재하지 않으면 ProductException 발생")
            void updateStatus_notFound() {
                // given
                Long productId = 999L;
                given(jpaRepository.findById(productId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> repository.updateStatus(productId, ProductStatus.ON_SALE))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.PRODUCT_NOT_FOUND_ERROR.getMessage());
            }
        }

        @Nested
        @DisplayName("상품 기본 정보")
        class UpdateBasicInfo {

            @Test
            @DisplayName("상품이 존재하면 기본 정보가 업데이트된다")
            void update_success() {
                // given
                Long productId = 1L;
                ProductEntity entity = createMockProductEntity(productId);
                Product domain = createMockProduct(1L, 100L, "새로운 의자");

                given(jpaRepository.findById(productId)).willReturn(Optional.of(entity));

                // when
                repository.update(productId, domain);

                // then
                assertThat(entity.getName()).isEqualTo("새로운 의자");
            }

            @Test
            @DisplayName("상품이 존재하지 않으면 ProductException 발생")
            void update_notFound() {
                // given
                Long productId = 999L;
                Product domain = createMockProduct(1L, 100L, "의자");

                given(jpaRepository.findById(productId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> repository.update(productId, domain))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.PRODUCT_NOT_FOUND_ERROR.getMessage());
            }
        }

        @Nested
        @DisplayName("상품 이미지")
        class UpdateMainImage {

            @Test
            @DisplayName("상품이 존재하면 대표 이미지가 업데이트된다")
            void updateMainImage_success() {
                // given
                Long productId = 1L;
                ProductEntity entity = createMockProductEntity(productId);

                given(jpaRepository.findById(productId)).willReturn(Optional.of(entity));

                // when
                repository.updateMainImage(productId, "https://new-image.jpg");

                // then
                assertThat(entity.getImageUrl()).isEqualTo("https://new-image.jpg");
            }

            @Test
            @DisplayName("상품이 존재하지 않으면 ProductException 발생")
            void updateMainImage_notFound() {
                // given
                Long productId = 999L;
                given(jpaRepository.findById(productId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> repository.updateMainImage(productId, "https://new-image.jpg"))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.PRODUCT_NOT_FOUND_ERROR.getMessage());
            }
        }

        @Nested
        @DisplayName("상품 상세 이미지")
        class UpdateDetailImage {
            @Nested
            @DisplayName("성공 케이스")
            class Success {

                @Test
                @DisplayName("상품이 존재하면 상세 이미지가 정상적으로 추가된다")
                void addDetailImages_success() {
                    // given
                    Long productId = 1L;
                    List<String> imageUrls = List.of(
                            "https://image1.jpg",
                            "https://image2.jpg"
                    );

                    ProductEntity entity = createMockProductEntity(productId);
                    given(jpaRepository.findById(productId)).willReturn(Optional.of(entity));

                    // when
                    repository.addDetailImages(productId, imageUrls);

                    assertThat(entity.getDetailImages()).hasSize(2);
                    assertThat(entity.getDetailImages())
                            .extracting(ProductDetailImageEntity::getImageUrl)
                            .containsExactly("https://image1.jpg", "https://image2.jpg");

                    // 양방향 연관관계 확인
                    assertThat(entity.getDetailImages().getFirst().getProduct()).isEqualTo(entity);
                }
            }

            @Nested
            @DisplayName("실패 케이스")
            class Fail {

                @Test
                @DisplayName("상품이 존재하지 않으면 ProductException 발생")
                void addDetailImages_productNotFound() {
                    // given
                    Long productId = 999L;
                    List<String> imageUrls = List.of("https://image1.jpg");
                    given(jpaRepository.findById(productId)).willReturn(Optional.empty());

                    // when & then
                    assertThatThrownBy(() -> repository.addDetailImages(productId, imageUrls))
                            .isInstanceOf(ProductException.class)
                            .hasMessage(ErrorCode.PRODUCT_NOT_FOUND_ERROR.getMessage());
                }
            }
        }
    }

    @Nested
    @DisplayName("상세 이미지 제거")
    class DeleteDetailImages {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("상품이 존재하면 removeDetailImagesByUrls가 호출된다")
            void deleteDetailImages_success() {
                // given
                Long productId = 1L;
                List<String> imageUrls = List.of("https://a.jpg", "https://b.jpg");
                ProductEntity entity = createMockProductEntity(productId);

                given(jpaRepository.findById(productId)).willReturn(Optional.of(entity));

                // when
                repository.deleteDetailImages(productId, imageUrls);

                // then
                assertThat(entity.getDetailImages()).isEmpty();
            }
        }

        @Nested
        @DisplayName("실패")
        class Fail {
            @Test
            @DisplayName("상품이 존재하지 않으면 ProductException 발생")
            void deleteDetailImages_notFound() {
                // given
                Long productId = 999L;
                given(jpaRepository.findById(productId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> repository.deleteDetailImages(productId, List.of("https://a.jpg")))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.PRODUCT_NOT_FOUND_ERROR.getMessage());
            }
        }
    }
}