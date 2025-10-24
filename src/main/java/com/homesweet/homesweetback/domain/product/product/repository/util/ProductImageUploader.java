package com.homesweet.homesweetback.domain.product.product.repository.util;

import com.homesweet.homesweetback.common.s3.ImageUploader;
import com.homesweet.homesweetback.domain.product.product.domain.ProductImages;
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
}
