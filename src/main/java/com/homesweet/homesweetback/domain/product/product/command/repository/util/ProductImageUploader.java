package com.homesweet.homesweetback.domain.product.product.command.repository.util;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.s3.ImageUploader;
import com.homesweet.homesweetback.domain.product.product.command.domain.ProductImages;
import com.homesweet.homesweetback.domain.product.product.command.domain.exception.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 제품 이미지 업로드 관련 코드
 *
 * @author junnukim1007gmail.com
 */
@Component
@RequiredArgsConstructor
public class ProductImageUploader {

    private static final int MAX_DETAIL_IMAGE_COUNT = 5;

    private final ImageUploader imageUploader;

    public ProductImages uploadProductImages(MultipartFile mainImage, List<MultipartFile> detailImages) {
        return new ProductImages(
                uploadSingle(mainImage, "product/main"),
                uploadMultiple(detailImages, "product/detail")
        );
    }

    public String uploadProductMainImage(MultipartFile mainImage) {
        return uploadSingle(mainImage, "product/main");
    }

    public String uploadProductReviewImage(MultipartFile image) {
        return uploadSingle(image, "product/review");
    }

    public List<String> uploadProductDetailImages(List<MultipartFile> detailImages) {
        return uploadMultiple(detailImages, "product/detail");
    }

    public void deleteImage(String imageUrl) {
        imageUploader.delete(imageUrl);
    }

    private String uploadSingle(MultipartFile file, String path) {
        return imageUploader.upload(file, path);
    }

    private List<String> uploadMultiple(List<MultipartFile> files, String path) {
        if (files == null || files.isEmpty()) return List.of();
        if (files.size() > MAX_DETAIL_IMAGE_COUNT)
            throw new ProductException(ErrorCode.EXCEEDED_IMAGE_LIMIT_ERROR);
        return imageUploader.uploadFiles(files, path);
    }
}
