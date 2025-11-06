package com.homesweet.homesweetback.domain.product.product.repository.util;

import com.homesweet.homesweetback.common.s3.ImageUploader;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
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

/**
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 6.
 */
@ExtendWith(MockitoExtension.class)
class ProductImageUploaderTest {

    @Mock
    private ImageUploader imageUploader;

    @InjectMocks
    private ProductImageUploader uploader;

    @Nested
    @DisplayName("성공")
    class Success {
        @Test
        @DisplayName("상세 이미지가 5장 이하라면 업로드가 정상 수행된다")
        void uploadProductDetailImages_WithinLimit() {
            // given
            List<MultipartFile> detailImages = List.of(
                    new MockMultipartFile("1", "1.jpg", "image/jpeg", "data".getBytes()),
                    new MockMultipartFile("2", "2.jpg", "image/jpeg", "data".getBytes())
            );

            given(imageUploader.uploadFiles(any(), any()))
                    .willReturn(List.of("url1", "url2"));

            // when
            List<String> urls = uploader.uploadProductDetailImages(detailImages);

            // then
            assertThat(urls).hasSize(2);
        }
    }

    @Nested
    @DisplayName("실패")
    class Fail {
        @Test
        @DisplayName("상세 이미지가 5장을 초과하면 예외가 발생한다")
        void uploadProductDetailImages_ExceedsLimit() {
            // given
            List<MultipartFile> detailImages = List.of(
                    new MockMultipartFile("1", "1.jpg", "image/jpeg", "data".getBytes()),
                    new MockMultipartFile("2", "2.jpg", "image/jpeg", "data".getBytes()),
                    new MockMultipartFile("3", "3.jpg", "image/jpeg", "data".getBytes()),
                    new MockMultipartFile("4", "4.jpg", "image/jpeg", "data".getBytes()),
                    new MockMultipartFile("5", "5.jpg", "image/jpeg", "data".getBytes()),
                    new MockMultipartFile("6", "6.jpg", "image/jpeg", "data".getBytes())
            );

            // when & then
            assertThatThrownBy(() -> uploader.uploadProductDetailImages(detailImages))
                    .isInstanceOf(ProductException.class)
                    .hasMessageContaining("상세 이미지는 최대 5장까지 업로드 가능합니다");
        }
    }
}