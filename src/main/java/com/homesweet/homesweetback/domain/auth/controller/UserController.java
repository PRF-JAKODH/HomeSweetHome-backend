package com.homesweet.homesweetback.domain.auth.controller;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.homesweet.homesweetback.domain.auth.service.UserService;

import jakarta.validation.Valid;

import com.homesweet.homesweetback.domain.auth.dto.UpdateUserRequest;
import com.homesweet.homesweetback.domain.auth.dto.UpdateUserRoleRequest;
import com.homesweet.homesweetback.domain.auth.dto.UserResponse;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    /**
     * 현재 사용자 정보 조회 API
     * JWT 토큰을 통해 인증된 사용자의 정보를 반환합니다.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal OAuth2UserPrincipal principal) {
        Long userId = principal.getUserId();
        UserResponse userResponse = userService.getUserInfo(userId);
        return ResponseEntity.ok(userResponse);
    }


    /**
     * 사용자 역할 수정 API
     * JWT 토큰을 통해 인증된 사용자의 역할을 수정합니다.
     */
    @PutMapping("/role")
    public ResponseEntity<UserResponse> updateUserRole(
        @Valid @RequestBody UpdateUserRoleRequest request,
        @AuthenticationPrincipal OAuth2UserPrincipal principal)
    {
        Long userId = principal.getUserId();
        UserResponse userResponse = userService.updateUserRole(userId, request);
        return ResponseEntity.ok(userResponse);
    }

    /**
     * 사용자 정보 수정 API
     * JWT 토큰을 통해 인증된 사용자의 정보를 수정합니다.
     * 프로필 이미지는 multipart로 받으며, 제공된 경우에만 업로드 및 수정됩니다.
     * 
     * @param request 사용자 정보 수정 요청 (JSON)
     * @param profileImage 프로필 이미지 파일 (선택사항, multipart)
     * @param principal 인증된 사용자 정보
     * @return 수정된 사용자 정보
     */
    @PutMapping(value = "/update", consumes = {"multipart/form-data"})
    public ResponseEntity<UserResponse> updateUser(
        @RequestPart("request") @Valid UpdateUserRequest request,
        @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
        @AuthenticationPrincipal OAuth2UserPrincipal principal) {
        
        Long userId = principal.getUserId();
        
        UserResponse userResponse = userService.updateUserInfo(userId, request, Optional.ofNullable(profileImage));
        return ResponseEntity.ok(userResponse);
    }
}
