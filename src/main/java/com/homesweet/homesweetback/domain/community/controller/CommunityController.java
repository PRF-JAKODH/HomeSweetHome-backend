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
     * JWT 토큰을 통해 인증된 사용자의 게시글을 작성합니다.
     *
     * @param request 게시글 작성 요청 데이터 (제목, 내용)
     * @param authentication Spring Security의 인증 정보 (JWT에서 자동 추출)
     * @return 생성된 게시글 정보
     */
    @PostMapping("/posts")
    public ResponseEntity<CommunityResponse> createPost(
            @Valid @RequestBody CommunityCreateRequest request,
            Authentication authentication
    ) {
        // JWT 토큰에서 인증된 사용자 정보 추출
        User currentUser = (User) authentication.getPrincipal();

        CommunityResponse response = communityService.createPost(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 게시글 작성 API (테스트용 - 인증 없음)
     * TODO: 실제 배포 전에 반드시 제거해야 합니다!
     *
     * @param request 게시글 작성 요청 데이터 (제목, 내용)
     * @param userId 테스트용 사용자 ID
     * @return 생성된 게시글 정보
     */
    @PostMapping("/posts/test")
    public ResponseEntity<CommunityResponse> createPostTest(
            @Valid @RequestBody CommunityCreateRequest request,
            @RequestParam Long userId
    ) {
        CommunityResponse response = communityService.createPost(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 게시글 단건 조회 API
     * GET /api/v1/community/posts/{postId}
     *
     * @param postId 조회할 게시글 ID
     * @return 게시글 정보
     */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<CommunityResponse> getPost(@PathVariable Long postId) {
        CommunityResponse response = communityService.getPost(postId);
        return ResponseEntity.ok(response);
    }
}

