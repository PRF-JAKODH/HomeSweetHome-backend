package com.homesweet.homesweetback.domain.product.product.command.controller.request.update;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 상품 이미지 업데이트 요청 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 30.
 */
public record ProductImageUpdateRequest(

        MultipartFile mainImage,

        @Size(max = 5, message = "상세 이미지는 최대 5개까지 업로드 할 수 있습니다")
        @NotNull(message = "상세 이미지 목록은 비어 있을 수 없습니다.")
        List<MultipartFile> detailImages,

        List<String> deleteDetailImageUrls
) {
    // null-safe 생성자
    public ProductImageUpdateRequest {
        // null일 경우 빈 리스트로 대체
        if (detailImages == null) {
            detailImages = List.of();
        }

        if (deleteDetailImageUrls == null) {
            deleteDetailImageUrls = List.of();
        }
    }

    // mainImage 존재 여부 확인용
    public boolean hasMainImage() {
        return mainImage != null && !mainImage.isEmpty();
    }

    // 상세 이미지 존재 여부 확인용
    public boolean hasDetailImages() {
        return !detailImages.isEmpty();
    }

    // 삭제할 상세 이미지 존재 여부 확인용
    public boolean hasDeleteTargets() {
        return !deleteDetailImageUrls.isEmpty();
    }
}
