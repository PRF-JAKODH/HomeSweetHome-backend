package com.homesweet.homesweetback.domain.auth.controller;

import com.homesweet.homesweetback.domain.auth.service.AuthService;
import com.homesweet.homesweetback.domain.auth.dto.AccessTokenResponse;
import com.homesweet.homesweetback.domain.auth.dto.SignUpResponse;
import com.homesweet.homesweetback.domain.auth.dto.SignupRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * 인증 관련 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 토큰 갱신 API
     * Cookie에서 Refresh Token을 추출하여 새로운 Access Token을 발급합니다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            AccessTokenResponse accessTokenResponse = authService.refreshToken(request, response);
            return ResponseEntity.ok(accessTokenResponse);
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }


    /**
     * 로그아웃 API
     * Refresh Token Cookie를 삭제하고 로그아웃 처리
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            authService.logout(request, response);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 회원가입 완료 API
     * OAuth2 로그인 후 핸드폰 번호, 생일, 역할을 설정합니다.
     */
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> completeSignup(
            @Valid @RequestBody SignupRequest signupRequest,
            HttpServletRequest request, 
            HttpServletResponse response) {
        try {
            SignUpResponse accessTokenResponse = authService.completeSignup(signupRequest, request, response);
            return ResponseEntity.ok(accessTokenResponse);
        } catch (IllegalArgumentException e) {
            log.error("Invalid signup data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            log.error("Failed to complete signup: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }
}
