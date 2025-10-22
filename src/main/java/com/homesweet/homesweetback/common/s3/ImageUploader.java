package com.homesweet.homesweetback.common.s3;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 이미지 업로드 서비스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
public interface ImageUploader {

    String upload(MultipartFile file, String fileName);

    List<String> uploadFiles(List<MultipartFile> files, String directory);

    void delete(String fileName);
}
