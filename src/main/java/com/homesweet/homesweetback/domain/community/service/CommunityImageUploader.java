package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.s3.ImageUploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CommunityImageUploader {

    private final ImageUploader imageUploader;

    public List<String> uploadCommunityImages(List<MultipartFile> images) {
        return imageUploader.uploadFiles(images, "community");

//      준우님이 적어주신거
//    public void uploadCommunityImage(List<MultipartFile> images) {
//        imageUploader.uploadFiles(images, "community");
    }


}
