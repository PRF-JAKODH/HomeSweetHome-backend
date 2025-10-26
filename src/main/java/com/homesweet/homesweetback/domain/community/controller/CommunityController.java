package com.homesweet.homesweetback.domain.community.controller;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.community.dto.CommunityCreateRequest;
import com.homesweet.homesweetback.domain.community.dto.CommunityResponse;
import com.homesweet.homesweetback.domain.community.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Community 컨트롤러
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */
@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    /**
     * 게시글 작성 API
     *
     */
    @PostMapping("/posts")
    public ResponseEntity<CommunityResponse> createPost(
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart("request") @Valid CommunityCreateRequest request,
            Authentication authentication
    ) {
        // 개발 중 임시 처리
        if (authentication == null) {
            // 테스트용 userId 사용
            CommunityResponse response = communityService.createPost(images, request, 1L);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        // JWT 토큰에서 인증된 사용자 정보 추출
        User currentUser = (User) authentication.getPrincipal();
        CommunityResponse response = communityService.createPost(images, request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }                                                   

    /**
     * 게시글 단건 조회 API
     */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<CommunityResponse> getPost(@PathVariable Long postId) {
        CommunityResponse response = communityService.getPost(postId);
        return ResponseEntity.ok(response);
    }
}

