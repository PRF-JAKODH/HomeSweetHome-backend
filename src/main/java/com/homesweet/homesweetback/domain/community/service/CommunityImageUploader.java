package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.s3.ImageUploader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CommunityImageUploader {

    private final ImageUploader imageUploader;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    public List<String> uploadCommunityImages(List<MultipartFile> images) {
        // 로컬 개발/테스트 환경에서는 S3 업로드 없이 가상 URL 반환
        if ("dev".equals(activeProfile) || "test".equals(activeProfile)) {
            return images.stream()
                .map(file -> "https://mock-s3-bucket.s3.ap-northeast-2.amazonaws.com/community/test-" + file.getOriginalFilename())
                .collect(Collectors.toList());
        }

        // 프로덕션 환경에서는 실제 S3 업로드
        return imageUploader.uploadFiles(images, "community");

//      준우님이 적어주신거
//    public void uploadCommunityImage(List<MultipartFile> images) {
//        imageUploader.uploadFiles(images, "community");
    }


}
