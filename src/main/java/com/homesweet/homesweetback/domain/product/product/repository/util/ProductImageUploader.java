package com.homesweet.homesweetback.domain.product.product.repository.util;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.s3.ImageUploader;
import com.homesweet.homesweetback.domain.product.product.domain.ProductImages;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.util.List;

/**
 * 제품 이미지 업로드 관련 코드
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
@Component
@RequiredArgsConstructor
public class ProductImageUploader {

    private static final int MAX_DETAIL_IMAGE_COUNT = 5;

    private final ImageUploader imageUploader;

    public ProductImages uploadProductImages(MultipartFile mainImage, List<MultipartFile> detailImages) {
        String mainImageUrl = imageUploader.upload(mainImage, "product/main");
        List<String> detailUrls = imageUploader.uploadFiles(detailImages, "product/detail");
        return new ProductImages(mainImageUrl, detailUrls);
    }

    public String uploadProductReviewImage(MultipartFile image) {
        return imageUploader.upload(image, "product/review");
    }

    public void deleteProductReviewImage(String imageUrl) {
        imageUploader.delete(imageUrl);
    }

    public String uploadProductMainImage(MultipartFile mainImage) {
        return imageUploader.upload(mainImage, "product/main");
    }

    public List<String> uploadProductDetailImages(List<MultipartFile> detailImages) {
        if (detailImages == null || detailImages.isEmpty()) {
            return List.of();
        }

        // 상세 이미지는 5개까지 가능
        if (detailImages.size() > MAX_DETAIL_IMAGE_COUNT) {
            throw new ProductException(ErrorCode.EXCEEDED_IMAGE_LIMIT_ERROR);
        }

        return imageUploader.uploadFiles(detailImages, "product/detail");
    }

    public void deleteProductImage(String imageUrl) {
        imageUploader.delete(imageUrl);
    }
}
