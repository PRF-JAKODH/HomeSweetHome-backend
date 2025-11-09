package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.s3.ImageUploader;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 이미지 업로드 테스트 설정
 * - In-Memory ImageUploader를 사용하여 실제 S3 업로드 없이 테스트
 */
@TestConfiguration
public class TestImageConfig {

    @Bean
    @Primary
    public ImageUploader testImageUploader() {
        return new InMemoryImageUploader();
    }
}
