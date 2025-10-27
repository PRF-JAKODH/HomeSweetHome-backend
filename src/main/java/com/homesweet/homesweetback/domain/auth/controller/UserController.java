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
import com.homesweet.homesweetback.domain.auth.entity.User;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
     */
    @PutMapping("/update")
    public ResponseEntity<UserResponse> updateUser(
        @Valid @RequestBody UpdateUserRequest request,
        @AuthenticationPrincipal OAuth2UserPrincipal principal) {
        Long userId = principal.getUserId();
        UserResponse userResponse = userService.updateUserInfo(userId, request);
        return ResponseEntity.ok(userResponse);
    }
}
