package com.homesweet.homesweetback.domain.community.controller;

import com.homesweet.homesweetback.domain.community.dto.CommunityCommentRequest;
import com.homesweet.homesweetback.domain.community.dto.CommunityCommentResponse;
import com.homesweet.homesweetback.domain.community.dto.CommunityPostRequest;
import com.homesweet.homesweetback.domain.community.dto.CommunityPostResponse;
import com.homesweet.homesweetback.domain.community.service.CommunityCommentService;
import com.homesweet.homesweetback.domain.community.service.CommunityCountService;
import com.homesweet.homesweetback.domain.community.service.CommunityPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;


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

    private final CommunityPostService CommunityPostService;
    private final CommunityCommentService CommunityCommentService;
    private final CommunityCountService CommunityCountService;

    /**
     * 게시글 작성 API
     *
     */
    @PostMapping("/posts")
    public ResponseEntity<CommunityPostResponse> createPost(
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart("request") @Valid CommunityPostRequest request,
            Authentication authentication
    ) {
        // 개발 중 임시 처리
        if (authentication == null) {
            // 테스트용 userId 사용
            CommunityPostResponse response = CommunityPostService.createPost(images, request, 1L);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        CommunityPostResponse response = CommunityPostService.createPost(images, request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 게시글 단건 조회 API
     */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<CommunityPostResponse> getPost(@PathVariable Long postId) {
        CommunityPostResponse response = CommunityPostService.getPost(postId);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 수정 API
     */
    @PutMapping("/posts/{postId}")
    public ResponseEntity<CommunityPostResponse> updatePost(
            @PathVariable Long postId,
            @RequestBody @Valid CommunityPostRequest request,
            Authentication authentication
    ) {
        // 개발 중 임시 처리
        if (authentication == null) {
            // 테스트용 userId 사용
            CommunityPostResponse response = CommunityPostService.updatePost(postId, request, 1L);
            return ResponseEntity.ok(response);
        }

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        CommunityPostResponse response = CommunityPostService.updatePost(postId, request, principal.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 삭제 API
     */
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        // 개발 중 임시 처리
        if (authentication == null) {
            // 테스트용 userId 사용
            CommunityPostService.deletePost(postId, 1L);
            return ResponseEntity.noContent().build();
        }

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        CommunityPostService.deletePost(postId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 댓글 작성 API
     */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommunityCommentResponse> createComment(
            @PathVariable Long postId,
            @RequestBody @Valid CommunityCommentRequest request,
            Authentication authentication
    ) {
        // 개발 중 임시 처리
        if (authentication == null) {
            // 테스트용 userId 사용
            CommunityCommentResponse response = CommunityCommentService.createComment(postId, request, 1L);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        CommunityCommentResponse response = CommunityCommentService.createComment(postId, request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 해당 게시글 모든 댓글 조회
     */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommunityCommentResponse>> getComments (
            @PathVariable Long postId
    ){
        List<CommunityCommentResponse> responses = CommunityCommentService.getCommentsByPostId(postId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 댓글 수정 API
     */
    @PutMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<CommunityCommentResponse> updateComment(
            @PathVariable Long commentId,
            @RequestBody @Valid CommunityCommentRequest request,
            Authentication authentication
    ) {
        // 개발 중 임시 처리
        if (authentication == null) {
            // 테스트용 userId 사용
            CommunityCommentResponse response = CommunityCommentService.updateComment(commentId, request, 1L);
            return ResponseEntity.ok(response);
        }

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        CommunityCommentResponse response = CommunityCommentService.updateComment(commentId, request, principal.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 삭제 API
     */
    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        // 개발 중 임시 처리
        if (authentication == null) {
            // 테스트용 userId 사용
            CommunityCommentService.deleteComment(commentId, postId, 1L);
            return ResponseEntity.noContent().build();
        }

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        CommunityCommentService.deleteComment(commentId, postId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 게시글 조회수 증가
     */
    @PostMapping("/posts/{postId}/views")
    public ResponseEntity<Void> increaseViewCount(@PathVariable Long postId) {
        CommunityCountService.increaseViewCount(postId);
        return ResponseEntity.ok().build();
    }

    /**
     * 게시글 좋아요 토글
     */
    @PostMapping("/posts/{postId}/likes")
    public ResponseEntity<Void> togglePostLike(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        if (authentication == null) {
            CommunityCountService.togglePostLike(postId, 1L);
            return ResponseEntity.ok().build();
        }

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        CommunityCountService.togglePostLike(postId, principal.getUserId());
        return ResponseEntity.ok().build();
    }

    /**
     * 게시글 좋아요 상태 확인
     */
    @GetMapping("/posts/{postId}/likes/status")
    public ResponseEntity<Boolean> getPostLikeStatus(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.ok(false);
        }

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        boolean isLiked = CommunityCountService.isPostLiked(postId, principal.getUserId());
        return ResponseEntity.ok(isLiked);
    }

    /**
     * 댓글 좋아요 토글
     */
    @PostMapping("/posts/{postId}/comments/{commentId}/likes")
    public ResponseEntity<Void> toggleCommentLike(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        if (authentication == null) {
            CommunityCountService.toggleCommentLike(commentId, 1L);
            return ResponseEntity.ok().build();
        }

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        CommunityCountService.toggleCommentLike(commentId, principal.getUserId());
        return ResponseEntity.ok().build();
    }

    /**
     * 댓글 좋아요 상태 확인
     */
    @GetMapping("/posts/{postId}/comments/{commentId}/likes/status")
    public ResponseEntity<Boolean> getCommentLikeStatus(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.ok(false);
        }

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        boolean isLiked = CommunityCountService.isCommentLiked(commentId, principal.getUserId());
        return ResponseEntity.ok(isLiked);
    }


    // 페이지네이션
    @GetMapping("/posts")
    public ResponseEntity<Page<CommunityPostResponse>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        // 소문자도 허용하도록 대문자로 변환
        Sort.Direction sortDirection = Sort.Direction.valueOf(direction.toUpperCase());

        Pageable pageable = PageRequest.of(page, size, sortDirection, sort);
        Page<CommunityPostResponse> posts = CommunityPostService.getPosts(pageable);
        return ResponseEntity.ok(posts);
    }
}