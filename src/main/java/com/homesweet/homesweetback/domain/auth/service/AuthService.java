package com.homesweet.homesweetback.domain.auth.service;

import com.homesweet.homesweetback.common.security.jwt.JwtTokenProvider;
import com.homesweet.homesweetback.common.util.CookieUtil;
import com.homesweet.homesweetback.domain.auth.dto.*;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserService userService;
    private final CookieUtil cookieUtil;

    /**
     * Refresh Token으로 새로운 Access Token을 발급합니다.
     */
    @Transactional
    public AccessTokenResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // Cookie에서 Refresh Token 추출
        String refreshToken = cookieUtil.getRefreshTokenFromCookie(request);
        
        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh token not found");
        }
        
        // Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            // 유효하지 않은 refresh token인 경우 cookie 삭제
            Cookie deleteCookie = cookieUtil.createRefreshTokenCookieForDeletion();
            response.addCookie(deleteCookie);
            throw new IllegalArgumentException("Invalid refresh token");
        }
        
        // 사용자 ID 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 새로운 Access Token과 Refresh Token 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(user);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user);
        
        // 새로운 Refresh Token을 Cookie에 설정
        Cookie refreshTokenCookie = cookieUtil.createRefreshTokenCookie(newRefreshToken);
        response.addCookie(refreshTokenCookie);
        
        log.info("Token refreshed successfully for user: {}", user.getEmail());
        
        return new AccessTokenResponse(newAccessToken);
    }

    /**
     * Refresh Token으로 사용자 정보를 조회합니다.
     */
    public User getUserFromRefreshToken(HttpServletRequest request) {
        String refreshToken = cookieUtil.getRefreshTokenFromCookie(request);
        
        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh token not found");
        }
        
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        return userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * 회원가입 완료 처리
     */
    @Transactional
    public SignUpResponse completeSignup(SignupRequest signupRequest, HttpServletRequest request, HttpServletResponse response) {
        // Refresh Token으로 사용자 조회
        User user = getUserFromRefreshToken(request);
        
        // 회원가입 완료 처리
        UserResponse updatedUser = userService.completeSignup(user.getId(), signupRequest);
        
        // 새로운 토큰 발급 (업데이트된 사용자 정보로)
        User updatedUserEntity = userRepository.findById(user.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        String newAccessToken = jwtTokenProvider.createAccessToken(updatedUserEntity);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(updatedUserEntity);
        
        // 새로운 Refresh Token을 Cookie에 설정
        Cookie refreshTokenCookie = cookieUtil.createRefreshTokenCookie(newRefreshToken);
        response.addCookie(refreshTokenCookie);
        
        log.info("User signup completed successfully: {}", user.getEmail());
        
        return new SignUpResponse(newAccessToken, updatedUser);
    }

    @Transactional
    public UserResponse updateUserRole(Long userId, UpdateUserRoleRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(request.role());
        return UserResponse.of(userRepository.save(user));
    }

    /**
     * 로그아웃 처리
     */
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        // Authorization 헤더에서 Access Token 추출
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            
            if (jwtTokenProvider.validateToken(accessToken)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);
                String email = jwtTokenProvider.getEmailFromToken(accessToken);
                
                log.info("User logout successful: userId={}, email={}", userId, email);
            }
        }
        
        // Refresh Token Cookie 삭제
        Cookie deleteCookie = cookieUtil.createRefreshTokenCookieForDeletion();
        response.addCookie(deleteCookie);
    }
}
