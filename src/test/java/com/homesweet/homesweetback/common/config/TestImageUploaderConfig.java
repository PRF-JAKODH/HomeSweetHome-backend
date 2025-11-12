package com.homesweet.homesweetback.common.config;

import com.homesweet.homesweetback.common.s3.ImageUploader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 테스트 환경용 이미지 업로드 서비스 설정
 * 
 * 실제 S3 연결 없이도 테스트가 가능하도록 하는 NoOp 구현체를 제공합니다.
 * 테스트 프로파일이 활성화될 때 자동으로 사용되며, 실제 S3 업로드 없이 동작합니다.
 */
@Configuration
@Profile("test")
public class TestImageUploaderConfig {

    @Bean
    @Primary
    public ImageUploader testImageUploader() {
        return new NoOpImageUploader();
    }

    /**
     * 테스트용 NoOp 이미지 업로드 서비스 구현체
     * 실제 S3 업로드 없이 동작하며, 테스트에서 S3 연결 없이도 정상 동작하도록 합니다.
     */
    private static class NoOpImageUploader implements ImageUploader {
        
        private static final String TEST_BASE_URL = "https://test.s3.amazonaws.com";
        
        @Override
        public String upload(MultipartFile file, String fileName) {
            String uniqueFileName = generateUniqueFileName(fileName, file.getOriginalFilename());
            String testUrl = TEST_BASE_URL + "/" + uniqueFileName;
            return testUrl;
        }

        @Override
        public List<String> uploadFiles(List<? extends MultipartFile> files, String directory) {
            if (files == null || files.isEmpty()) {
                return new ArrayList<>();
            }

            List<String> uploadedUrls = new ArrayList<>();
            for (MultipartFile file : files) {
                String uploadedUrl = upload(file, directory);
                uploadedUrls.add(uploadedUrl);
            }
            
            return uploadedUrls;
        }

        @Override
        public void delete(String fileName) {
            // 테스트 환경에서는 실제 S3 삭제 없이 로그만 남깁니다.
            // 이렇게 하면 테스트에서 S3 연결 없이도 ImageUploader를 사용하는 서비스가 정상 동작합니다.
        }

        /**
         * UUID 기반 파일명 생성 (실제 구현과 동일한 로직)
         */
        private String generateUniqueFileName(String directory, String originalFilename) {
            String uuid = UUID.randomUUID().toString();
            return directory + "/" + uuid + "_" + originalFilename;
        }
    }
}

