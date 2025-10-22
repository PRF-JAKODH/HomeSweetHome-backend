package com.homesweet.homesweetback.common.s3.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.s3.ImageUploader;
import com.homesweet.homesweetback.common.s3.exception.CustomS3Exception;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Exception;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * S3 이미지 업로드 서비스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3ImageUploader implements ImageUploader {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * 단일 이미지 업로드
     */
    @Override
    public String upload(MultipartFile file, String fileName) {
        try (InputStream is = file.getInputStream()) {
            String uniqueFileName = generateUniqueFileName(fileName, file.getOriginalFilename());

            S3Resource uploaded = s3Template.upload(
                    bucketName,
                    uniqueFileName,
                    is,
                    ObjectMetadata.builder()
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("S3 업로드 완료: {}", uploaded.getURL());
            return uploaded.getURL().toString();

        } catch (IOException e) {
            log.error("파일 스트림 처리 중 오류", e);
            throw new CustomS3Exception(ErrorCode.FILE_STREAM_ERROR);
        } catch (S3Exception e) {
            log.error("S3 업로드 실패", e);
            throw new CustomS3Exception(ErrorCode.FAILED_UPLOAD_S3_ERROR);
        }
    }

    /**
     * 다중 이미지 업로드
     */
    @Override
    public List<String> uploadFiles(List<MultipartFile> files, String directory) {
        if (files == null || files.isEmpty()) {
            throw new CustomS3Exception(ErrorCode.INVALID_FILE_ERROR);
        }

        List<String> uploadedUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileName = directory + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            String uploadedUrl = upload(file, fileName);
            uploadedUrls.add(uploadedUrl);
        }

        return uploadedUrls;
    }

    /**
     * 이미지 삭제
     */
    @Override
    public void delete(String fileName) {
        try {
            s3Template.deleteObject(bucketName, fileName);
            log.info("S3 이미지 삭제 완료: {}", fileName);
        } catch (S3Exception e) {
            log.error("S3 이미지 삭제 실패", e);
            throw new CustomS3Exception(ErrorCode.CANNOT_FOUND_S3_ERROR);
        }
    }

    /**
     * UUID 기반 파일명 생성
     */
    private String generateUniqueFileName(String directory, String originalFilename) {
        String uuid = UUID.randomUUID().toString();
        return directory + "/" + uuid + "_" + originalFilename;
    }
}
