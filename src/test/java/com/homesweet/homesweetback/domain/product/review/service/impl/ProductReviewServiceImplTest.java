package com.homesweet.homesweetback.domain.product.review.service.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.util.ScrollResponse;
import com.homesweet.homesweetback.common.valid.ProductValidator;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.notification.service.NotificationSendService;
import com.homesweet.homesweetback.domain.product.product.domain.Product;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.repository.ProductRepository;
import com.homesweet.homesweetback.domain.product.product.repository.util.ProductImageUploader;
import com.homesweet.homesweetback.domain.product.review.controller.request.ProductReviewCreateRequest;
import com.homesweet.homesweetback.domain.product.review.controller.request.ProductReviewUpdateRequest;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewResponse;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewStatisticsResponse;
import com.homesweet.homesweetback.domain.product.review.domain.ProductReview;
import com.homesweet.homesweetback.domain.product.review.repository.ProductReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static com.homesweet.homesweetback.domain.product.data.ProductMockData.*;
import static com.homesweet.homesweetback.domain.product.data.ProductReviewMockData.*;
import static com.homesweet.homesweetback.domain.product.data.UserMockData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

/**
 *
 * @author junnukim1007gmail.com
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("상품 리뷰 작성 서비스 테스트")
class ProductReviewServiceImplTest {

    @InjectMocks
    private ProductReviewServiceImpl service;

    @Mock
    private ProductValidator productValidator;
    @Mock
    private ProductReviewRepository productReviewRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductImageUploader imageUploader;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationSendService notificationSendService;

    @Nested
    @DisplayName("상품 리뷰 생성")
    class CreateReview {
        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("이미지 없이 리뷰를 작성할 수 있다")
            void createReview_withoutImage() {
                // given
                Long productId = 1L;
                Long userId = 2L;
                ProductReviewCreateRequest request = new ProductReviewCreateRequest(5, "좋아요!", null);

                ProductReview saved = createMockReview(productId, userId, 5, "좋아요!", null);
                Product product = createMockProduct(productId, 100L, "테이블");
                User seller = createMockUser(100L, "판매자");

                willDoNothing().given(productValidator).validateExistsProduct(productId);
                willDoNothing().given(productValidator).validateDuplicateReview(productId, userId);
                given(productReviewRepository.save(any(ProductReview.class))).willReturn(saved);
                given(productRepository.findByProductId(productId)).willReturn(product);
                given(userRepository.findById(product.getSellerId())).willReturn(Optional.of(seller));
                willDoNothing().given(notificationSendService)
                        .sendTemplateNotificationToSingleUser(anyLong(), any(), any());

                // when
                ProductReviewResponse response = service.createReview(productId, userId, request);

                // then
                assertThat(response.rating()).isEqualTo(5);
                assertThat(response.comment()).isEqualTo("좋아요!");
                assertThat(response.reviewImageUrl()).isNull();
            }

            @Test
            @DisplayName("이미지를 포함하여 리뷰를 작성할 수 있다")
            void createReview_withImage() {
                // given
                Long productId = 1L;
                Long userId = 2L;
                MockMultipartFile image = new MockMultipartFile("file", "image.jpg", "image/jpeg", "data".getBytes());
                ProductReviewCreateRequest request = new ProductReviewCreateRequest(4, "사진과 함께", image);

                ProductReview saved = createMockReview(productId, userId, 4, "사진과 함께", "https://s3.aws/review.jpg");
                Product product = createMockProduct(productId, 100L, "의자");
                User seller = createMockUser(100L, "판매자");

                willDoNothing().given(productValidator).validateExistsProduct(productId);
                willDoNothing().given(productValidator).validateDuplicateReview(productId, userId);
                willDoNothing().given(notificationSendService)
                        .sendTemplateNotificationToSingleUser(anyLong(), any(), any());
                given(imageUploader.uploadProductReviewImage(image)).willReturn("https://s3.aws/review.jpg");
                given(productReviewRepository.save(any(ProductReview.class))).willReturn(saved);
                given(productRepository.findByProductId(productId)).willReturn(product);
                given(userRepository.findById(product.getSellerId())).willReturn(Optional.of(seller));

                // when
                ProductReviewResponse response = service.createReview(productId, userId, request);

                // then
                assertThat(response.rating()).isEqualTo(4);
                assertThat(response.reviewImageUrl()).isEqualTo("https://s3.aws/review.jpg");
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Fail {

            @Test
            @DisplayName("상품이 존재하지 않으면 ProductException 발생")
            void createReview_productNotFound() {
                // given
                Long productId = 999L;
                Long userId = 1L;
                ProductReviewCreateRequest request = new ProductReviewCreateRequest(5, "리뷰", null);

                willThrow(new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR))
                        .given(productValidator).validateExistsProduct(productId);

                // when & then
                assertThatThrownBy(() -> service.createReview(productId, userId, request))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.PRODUCT_NOT_FOUND_ERROR.getMessage());
            }

            @Test
            @DisplayName("중복 리뷰 작성 시 ProductException 발생")
            void createReview_duplicateReview() {
                // given
                Long productId = 1L;
                Long userId = 2L;
                ProductReviewCreateRequest request = new ProductReviewCreateRequest(5, "중복리뷰", null);

                willDoNothing().given(productValidator).validateExistsProduct(productId);
                // 특정 제품에 이미 리뷰를 작성했다고 가정 -> 실제 확인은 통합 테스트에서 할 예정
                willThrow(new ProductException(ErrorCode.ALREADY_REVIEW_EXISTS))
                        .given(productValidator).validateDuplicateReview(productId, userId);

                // when & then
                assertThatThrownBy(() -> service.createReview(productId, userId, request))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.ALREADY_REVIEW_EXISTS.getMessage());
            }

            @Test
            @DisplayName("판매자 정보가 존재하지 않으면 BusinessException 발생")
            void createReview_sellerNotFound() {
                // given
                Long productId = 1L;
                Long userId = 2L;
                ProductReviewCreateRequest request = new ProductReviewCreateRequest(5, "좋아요", null);

                ProductReview saved = createMockReview(productId, userId, 5, "좋아요", null);
                Product product = createMockProduct(productId, 999L, "의자");

                willDoNothing().given(productValidator).validateExistsProduct(productId);
                willDoNothing().given(productValidator).validateDuplicateReview(productId, userId);
                given(productReviewRepository.save(any(ProductReview.class))).willReturn(saved);
                given(productRepository.findByProductId(productId)).willReturn(product);
                // 판매자 정보가 null일 경우
                given(userRepository.findById(product.getSellerId())).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> service.createReview(productId, userId, request))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("상품 리뷰 무한 스크롤")
    class GetProductReviews {
        @Test
        @DisplayName("마지막 페이지일 경우 hasNext=false, nextCursorId=null")
        void getProductReviews_lastPage() {
            // given
            Long productId = 1L;
            Long cursorId = null;
            int size = 3;

            List<ProductReviewResponse> reviews = List.of(
                    createReviewResponse(10L, productId, 1L, 5, "좋아요"),
                    createReviewResponse(9L, productId, 2L, 4, "보통이에요")
            );

            given(productReviewRepository.findNextReviews(productId, cursorId, size + 1))
                    .willReturn(reviews);

            // when
            ScrollResponse<ProductReviewResponse> result = service.getProductReviews(productId, cursorId, size);

            // then
            assertThat(result.contents()).hasSize(2);
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursorId()).isNull();
        }

        @Test
        @DisplayName("다음 페이지가 존재할 경우 hasNext=true, nextCursorId=마지막 리뷰 ID")
        void getProductReviews_hasNextPage() {
            // given
            Long productId = 1L;
            Long cursorId = 10L;
            int size = 2;

            List<ProductReviewResponse> reviews = List.of(
                    createReviewResponse(9L, productId, 1L, 5, "좋아요"),
                    createReviewResponse(8L, productId, 2L, 4, "괜찮아요"),
                    createReviewResponse(7L, productId, 3L, 3, "그냥 그래요")
            );

            given(productReviewRepository.findNextReviews(productId, cursorId, size + 1))
                    .willReturn(reviews);

            // when
            ScrollResponse<ProductReviewResponse> result = service.getProductReviews(productId, cursorId, size);

            // then
            assertThat(result.contents()).hasSize(size);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursorId()).isEqualTo(8L);
        }

        @Test
        @DisplayName("조회 결과가 비어있으면 빈 리스트와 hasNext=false를 반환한다")
        void getProductReviews_emptyResult() {
            // given
            Long productId = 1L;
            Long cursorId = null;
            int size = 3;

            given(productReviewRepository.findNextReviews(productId, cursorId, size + 1))
                    .willReturn(List.of());

            // when
            ScrollResponse<ProductReviewResponse> result = service.getProductReviews(productId, cursorId, size);

            // then
            assertThat(result.contents()).isEmpty();
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursorId()).isNull();
        }
    }

    @Nested
    @DisplayName("사용자 작성 상품 리뷰 조회")
    class GetWriterProductReviews {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("마지막 페이지일 경우 hasNext=false, nextCursorId=null")
            void getUserReviews_lastPage() {
                // given
                Long userId = 2L;
                Long cursorId = null;
                int limit = 3;

                List<ProductReviewResponse> reviews = List.of(
                        createReviewResponse(11L, 100L, userId, 5, "만족합니다."),
                        createReviewResponse(10L, 101L, userId, 4, "괜찮아요.")
                );

                given(productReviewRepository.findNextUserReviews(userId, cursorId, limit + 1))
                        .willReturn(reviews);

                // when
                ScrollResponse<ProductReviewResponse> result = service.getUserReviews(userId, cursorId, limit);

                // then
                assertThat(result.contents()).hasSize(2);
                assertThat(result.hasNext()).isFalse();
                assertThat(result.nextCursorId()).isNull();
            }

            @Test
            @DisplayName("다음 페이지가 존재할 경우 hasNext=true, nextCursorId=마지막 리뷰 ID")
            void getUserReviews_hasNextPage() {
                // given
                Long userId = 2L;
                Long cursorId = 10L;
                int limit = 2;

                List<ProductReviewResponse> reviews = List.of(
                        createReviewResponse(9L, 100L, userId, 5, "좋아요!"),
                        createReviewResponse(8L, 101L, userId, 4, "괜찮아요!"),
                        createReviewResponse(7L, 102L, userId, 3, "그냥 그래요.")
                );

                given(productReviewRepository.findNextUserReviews(userId, cursorId, limit + 1))
                        .willReturn(reviews);

                // when
                ScrollResponse<ProductReviewResponse> result = service.getUserReviews(userId, cursorId, limit);

                // then
                assertThat(result.contents()).hasSize(limit);
                assertThat(result.hasNext()).isTrue();
                assertThat(result.nextCursorId()).isEqualTo(8L);
            }

            @Test
            @DisplayName("조회 결과가 비어있으면 빈 리스트와 hasNext=false를 반환한다")
            void getUserReviews_emptyResult() {
                // given
                Long userId = 2L;
                Long cursorId = null;
                int limit = 3;

                given(productReviewRepository.findNextUserReviews(userId, cursorId, limit + 1))
                        .willReturn(List.of());

                // when
                ScrollResponse<ProductReviewResponse> result = service.getUserReviews(userId, cursorId, limit);

                // then
                assertThat(result.contents()).isEmpty();
                assertThat(result.hasNext()).isFalse();
                assertThat(result.nextCursorId()).isNull();
            }
        }
    }

    @Nested
    @DisplayName("상품 리뷰 통계 정보 조회")
    class GetReviewStatistics {
        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("상품 리뷰 통계를 정상적으로 조회할 수 있다")
            void getReviewStatistics_success() {
                // given
                Long productId = 1L;

                ProductReviewStatisticsResponse mockResponse = createReviewStatisticsResponse(productId);

                given(productReviewRepository.getReviewStatistics(productId))
                        .willReturn(mockResponse);

                // when
                ProductReviewStatisticsResponse result = service.getReviewStatistics(productId);

                // then
                assertThat(result).isNotNull();
                assertThat(result.productId()).isEqualTo(productId);
                assertThat(result.totalCount()).isEqualTo(10L);
                assertThat(result.averageRating()).isEqualTo(4.5);
                assertThat(result.ratingCounts()).containsEntry(5, 6L)
                        .containsEntry(4, 3L)
                        .containsEntry(3, 1L);
            }

            @Test
            @DisplayName("리뷰가 없는 상품은 카운트 0과 평균 0.0으로 반환할 수 있다")
            void getReviewStatistics_emptyProduct() {
                // given
                Long productId = 99L;

                ProductReviewStatisticsResponse emptyStats = createEmptyReviewStatisticsResponse(productId);

                given(productReviewRepository.getReviewStatistics(productId))
                        .willReturn(emptyStats);

                // when
                ProductReviewStatisticsResponse result = service.getReviewStatistics(productId);

                // then
                assertThat(result.productId()).isEqualTo(productId);
                assertThat(result.totalCount()).isZero();
                assertThat(result.averageRating()).isEqualTo(0.0);
                assertThat(result.ratingCounts()).isEmpty();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Fail {

            @Test
            @DisplayName("리포지토리가 null을 반환하면 null이 그대로 반환된다")
            void getReviewStatistics_nullReturn() {
                // given
                Long productId = 1L;
                given(productReviewRepository.getReviewStatistics(anyLong())).willReturn(null);

                // when
                ProductReviewStatisticsResponse result = service.getReviewStatistics(productId);

                // then
                assertThat(result).isNull();
            }
        }
    }

    @Nested
    @DisplayName("상품 리뷰 업데이트")
    class ProductReviewUpdate {
        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("이미지를 변경하지 않고 리뷰 내용을 수정할 수 있다")
            void updateReview_withoutImage() {
                // given
                Long reviewId = 1L;
                Long userId = 2L;
                ProductReview existing = createMockReview(reviewId, 100L, userId, "https://s3.aws/old.jpg");

                ProductReviewUpdateRequest request = createProductReviewUpdateRequest(5, "수정된 리뷰", null);
                ProductReview updated = existing.update(request.rating(), request.comment(), existing.imageUrl());

                given(productReviewRepository.findById(reviewId)).willReturn(Optional.of(existing));
                willDoNothing().given(productValidator).validateDuplicateWriter(existing, userId);
                given(productReviewRepository.update(any(ProductReview.class))).willReturn(updated);

                // when
                ProductReviewResponse response = service.updateReview(reviewId, userId, request);

                // then
                assertThat(response.rating()).isEqualTo(5);
                assertThat(response.comment()).isEqualTo("수정된 리뷰");
                assertThat(response.reviewImageUrl()).isEqualTo(existing.imageUrl());
            }

            @Test
            @DisplayName("기존 이미지가 없을 때 새 이미지를 업로드할 수 있다")
            void updateReview_addImage() {
                // given
                Long reviewId = 1L;
                Long userId = 2L;
                ProductReview existing = createMockReview(reviewId, 100L, userId, null);

                MockMultipartFile newImage = new MockMultipartFile("image", "new.jpg", "image/jpeg", "data".getBytes());
                ProductReviewUpdateRequest request = createProductReviewUpdateRequest(4, "이미지 추가", newImage);

                String uploadedUrl = "https://s3.aws/new.jpg";
                ProductReview updated = existing.update(request.rating(), request.comment(), uploadedUrl);

                given(productReviewRepository.findById(reviewId)).willReturn(Optional.of(existing));
                willDoNothing().given(productValidator).validateDuplicateWriter(existing, userId);
                given(imageUploader.uploadProductReviewImage(newImage)).willReturn(uploadedUrl);
                given(productReviewRepository.update(any(ProductReview.class))).willReturn(updated);

                // when
                ProductReviewResponse response = service.updateReview(reviewId, userId, request);

                // then
                assertThat(response.reviewImageUrl()).isEqualTo(uploadedUrl);
            }

            @Test
            @DisplayName("기존 이미지를 교체할 수 있다 (기존 이미지 삭제 후 새 이미지 업로드)")
            void updateReview_replaceImage() {
                // given
                Long reviewId = 1L;
                Long userId = 2L;
                ProductReview existing = createMockReview(reviewId, 100L, userId, "https://s3.aws/old.jpg");

                MockMultipartFile newImage = new MockMultipartFile("image", "new.jpg", "image/jpeg", "data".getBytes());
                ProductReviewUpdateRequest request = createProductReviewUpdateRequest(5, "교체된 리뷰", newImage);

                String newImageUrl = "https://s3.aws/new.jpg";
                ProductReview updated = existing.update(request.rating(), request.comment(), newImageUrl);

                given(productReviewRepository.findById(reviewId)).willReturn(Optional.of(existing));
                willDoNothing().given(productValidator).validateDuplicateWriter(existing, userId);
                willDoNothing().given(imageUploader).deleteImage(existing.imageUrl());
                given(imageUploader.uploadProductReviewImage(newImage)).willReturn(newImageUrl);
                given(productReviewRepository.update(any(ProductReview.class))).willReturn(updated);

                // when
                ProductReviewResponse response = service.updateReview(reviewId, userId, request);

                // then
                assertThat(response.reviewImageUrl()).isEqualTo(newImageUrl);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Fail {

            @Test
            @DisplayName("리뷰가 존재하지 않으면 ProductException 발생")
            void updateReview_notFound() {
                // given
                Long reviewId = 99L;
                Long userId = 1L;
                ProductReviewUpdateRequest request = new ProductReviewUpdateRequest(5, "리뷰 없음", null);

                given(productReviewRepository.findById(reviewId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> service.updateReview(reviewId, userId, request))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.PRODUCT_REVIEW_NOT_FOUND_ERROR.getMessage());
            }

            @Test
            @DisplayName("작성자가 아닌 경우 ProductException 발생")
            void updateReview_unauthorizedWriter() {
                // given
                Long reviewId = 1L;
                Long userId = 999L; // 다른 사용자
                ProductReview existing = createMockReview(reviewId, 100L, 2L, null);

                ProductReviewUpdateRequest request = new ProductReviewUpdateRequest(5, "권한 없음", null);

                given(productReviewRepository.findById(reviewId)).willReturn(Optional.of(existing));
                willThrow(new ProductException(ErrorCode.PRODUCT_REVIEW_FORBIDDEN))
                        .given(productValidator).validateDuplicateWriter(existing, userId);

                // when & then
                assertThatThrownBy(() -> service.updateReview(reviewId, userId, request))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.PRODUCT_REVIEW_FORBIDDEN.getMessage());
            }
        }
    }
}