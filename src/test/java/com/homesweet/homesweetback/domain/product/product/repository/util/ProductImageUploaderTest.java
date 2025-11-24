package com.homesweet.homesweetback.domain.product.product.repository.util;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.s3.ImageUploader;
import com.homesweet.homesweetback.domain.product.product.command.domain.ProductImages;
import com.homesweet.homesweetback.domain.product.product.command.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.command.repository.util.ProductImageUploader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;

/**
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 6.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("상품 이미지 업로더 단위 테스트")
class ProductImageUploaderTest {

    @Mock
    private ImageUploader imageUploader;

    @InjectMocks
    private ProductImageUploader uploader;

    private MockMultipartFile mockFile(String name) {
        return new MockMultipartFile(name, (name + ".jpg"), "image/jpeg", "dummy".getBytes());
    }

    @Nested
    @DisplayName("상품 이미지 업로드")
    class UploadProductImages {

        @Test
        @DisplayName("메인 + 상세 이미지 업로드 성공 시 ProductImages 반환")
        void uploadProductImages_success() {
            // given
            MockMultipartFile mainImage = mockFile("main");
            List<MockMultipartFile> detailImages = List.of(mockFile("detail1"), mockFile("detail2"));

            given(imageUploader.upload(mainImage, "product/main"))
                    .willReturn("https://s3.aws/main.jpg");
            given(imageUploader.uploadFiles((List<MultipartFile>)(List<?>) detailImages, "product/detail"))
                    .willReturn(List.of("https://s3.aws/detail1.jpg", "https://s3.aws/detail2.jpg"));

            // when
            ProductImages result = uploader.uploadProductImages(mainImage, (List<MultipartFile>)(List<?>) detailImages);

            // then
            assertThat(result.mainImageUrl()).isEqualTo("https://s3.aws/main.jpg");
            assertThat(result.detailImageUrls())
                    .containsExactly("https://s3.aws/detail1.jpg", "https://s3.aws/detail2.jpg");
            verify(imageUploader).upload(mainImage, "product/main");
            verify(imageUploader).uploadFiles(detailImages, "product/detail");
        }

        @Test
        @DisplayName("상세 이미지가 5개 초과되면 ProductException 발생")
        void uploadProductImages_exceedLimit() {
            // given
            List<MockMultipartFile> detailImages = List.of(
                    mockFile("1"), mockFile("2"), mockFile("3"),
                    mockFile("4"), mockFile("5"), mockFile("6")
            );

            // when & then
            assertThatThrownBy(() -> uploader.uploadProductDetailImages((List<MultipartFile>)(List<?>) detailImages))
                    .isInstanceOf(ProductException.class)
                    .hasMessageContaining(ErrorCode.EXCEEDED_IMAGE_LIMIT_ERROR.getMessage());
        }
    }

    @Nested
    @DisplayName("단일 이미지 업로드")
    class SingleImageUpload {

        @Test
        @DisplayName("상품 메인 이미지 업로드 성공")
        void uploadMainImage_success() {
            // given
            MockMultipartFile mainImage = mockFile("main");
            given(imageUploader.upload(mainImage, "product/main"))
                    .willReturn("https://s3.aws/main.jpg");

            // when
            String url = uploader.uploadProductMainImage(mainImage);

            // then
            assertThat(url).isEqualTo("https://s3.aws/main.jpg");
            verify(imageUploader).upload(mainImage, "product/main");
        }

        @Test
        @DisplayName("상품 리뷰 이미지 업로드 성공")
        void uploadReviewImage_success() {
            // given
            MockMultipartFile image = mockFile("review");
            given(imageUploader.upload(image, "product/review"))
                    .willReturn("https://s3.aws/review.jpg");

            // when
            String url = uploader.uploadProductReviewImage(image);

            // then
            assertThat(url).isEqualTo("https://s3.aws/review.jpg");
        }
    }

    @Nested
    @DisplayName("이미지 삭제")
    class DeleteImage {

        @Test
        @DisplayName("이미지 URL이 주어지면 삭제 메서드 호출")
        void deleteImage_success() {
            // given
            String imageUrl = "https://s3.aws/delete.jpg";
            willDoNothing().given(imageUploader).delete(imageUrl);

            // when
            uploader.deleteImage(imageUrl);

            // then
            verify(imageUploader).delete(imageUrl);
        }
    }

    @Nested
    @DisplayName("상세 이미지 업로드")
    class DetailImageUpload {

        @Test
        @DisplayName("상세 이미지가 없으면 빈 리스트 반환")
        void uploadDetailImages_empty() {
            // when
            List<String> result = uploader.uploadProductDetailImages(List.of());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("상세 이미지 3개 업로드 성공")
        void uploadDetailImages_success() {
            // given
            List<MockMultipartFile> detailImages = List.of(
                    mockFile("1"), mockFile("2"), mockFile("3")
            );
            given(imageUploader.uploadFiles(detailImages, "product/detail"))
                    .willReturn(List.of("url1", "url2", "url3"));

            // when
            List<String> result = uploader.uploadProductDetailImages((List<MultipartFile>)(List<?>) detailImages);

            // then
            assertThat(result).containsExactly("url1", "url2", "url3");
            verify(imageUploader).uploadFiles(detailImages, "product/detail");
        }
    }
}