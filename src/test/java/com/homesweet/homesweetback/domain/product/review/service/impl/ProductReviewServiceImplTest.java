package com.homesweet.homesweetback.domain.product.review.service.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.valid.ProductValidator;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.notification.service.NotificationSendService;
import com.homesweet.homesweetback.domain.product.product.domain.Product;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.repository.ProductRepository;
import com.homesweet.homesweetback.domain.product.product.repository.util.ProductImageUploader;
import com.homesweet.homesweetback.domain.product.review.controller.request.ProductReviewCreateRequest;
import com.homesweet.homesweetback.domain.product.review.controller.response.ProductReviewResponse;
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

import java.util.Optional;

import static com.homesweet.homesweetback.domain.product.data.ProductMockData.*;
import static com.homesweet.homesweetback.domain.product.data.ProductReviewMockData.*;
import static com.homesweet.homesweetback.domain.product.data.UserMockData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;

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

                verify(productValidator).validateExistsProduct(productId);
                verify(productValidator).validateDuplicateReview(productId, userId);
                verify(imageUploader, never()).uploadProductReviewImage(any());
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
                verify(imageUploader).uploadProductReviewImage(image);
                verify(productReviewRepository).save(any(ProductReview.class));
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

                verify(productValidator).validateExistsProduct(productId);
                verify(productValidator, never()).validateDuplicateReview(anyLong(), anyLong());
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

                verify(productValidator).validateExistsProduct(productId);
                verify(productValidator).validateDuplicateReview(productId, userId);
                verifyNoInteractions(imageUploader);
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
}