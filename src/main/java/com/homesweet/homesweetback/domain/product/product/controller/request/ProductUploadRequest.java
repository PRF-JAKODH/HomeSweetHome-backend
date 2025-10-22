package com.homesweet.homesweetback.domain.product.product.controller.request;

import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.annotations.NotNull;

import java.util.List;

/**
 * 상품 등록 + 이미지 업로드 요청 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
public record ProductUploadRequest(
        ProductCreateRequest product,
        @NotNull
        MultipartFile mainImage,
        @Size(max = 5, message = "상세 이미지는 최대 5개까지 업로드 할 수 있습니다")
        List<MultipartFile> detailImages
) {
}
