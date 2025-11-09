package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.s3.ImageUploader;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 테스트용 In-Memory 이미지 업로더
 * - 실제 S3 업로드 대신 메모리에 저장
 * - 빠른 테스트 실행과 외부 의존성 제거
 */
public class InMemoryImageUploader implements ImageUploader {

    // 업로드된 이미지 정보를 메모리에 저장
    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();
    private int imageCounter = 0;

    @Override
    public String upload(MultipartFile file, String fileName) {
        try {
            String imageUrl = "https://test-bucket.s3.amazonaws.com/" + fileName;
            storage.put(imageUrl, file.getBytes());
            return imageUrl;
        } catch (Exception e) {
            throw new RuntimeException("이미지 업로드 실패", e);
        }
    }

    @Override
    public List<String> uploadFiles(List<MultipartFile> files, String directory) {
        List<String> uploadedUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileName = directory + "/" + (++imageCounter) + "_" + file.getOriginalFilename();
            String url = upload(file, fileName);
            uploadedUrls.add(url);
        }

        return uploadedUrls;
    }

    @Override
    public void delete(String fileName) {
        storage.remove(fileName);
    }

    // 테스트용 헬퍼 메소드
    public boolean isUploaded(String url) {
        return storage.containsKey(url);
    }

    public int getUploadCount() {
        return storage.size();
    }

    public void clear() {
        storage.clear();
        imageCounter = 0;
    }
}
