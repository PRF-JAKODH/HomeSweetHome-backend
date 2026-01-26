package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.s3.ImageUploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * [커뮤니티 이미지 업로더 - S3에 이미지 업로드]
 *
 * [하는 일]
 * 게시글에 첨부된 이미지를 AWS S3(클라우드 저장소)에 업로드
 *
 * [왜 별도 클래스로 분리?]
 * - 공통 ImageUploader를 감싸서 "community" 폴더에 저장하도록 고정
 * - 나중에 커뮤니티 전용 로직(이미지 리사이징 등)을 추가하기 쉬움
 */
@Component // 스프링 빈으로 등록
@RequiredArgsConstructor // final 필드 자동 생성자 주입
public class CommunityImageUploader {

    // 공통 이미지 업로더 (S3 업로드 담당)
    private final ImageUploader imageUploader;

    /**
     * [커뮤니티 이미지 업로드]
     *
     * @param images 업로드할 이미지 파일 리스트
     * @return 업로드된 이미지의 S3 URL 리스트
     *
     *         예: ["https://s3.../community/abc123.jpg",
     *         "https://s3.../community/def456.png"]
     */
    public List<String> uploadCommunityImages(List<MultipartFile> images) {
        // "community" 폴더에 이미지 업로드
        return imageUploader.uploadFiles(images, "community");
    }
}
