package com.homesweet.homesweetback.domain.auth.service;

import com.homesweet.homesweetback.common.security.jwt.JwtTokenProvider;
import com.homesweet.homesweetback.common.util.CookieUtil;
import com.homesweet.homesweetback.domain.auth.dto.*;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.common.util.PhoneNumberValidator;

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
     * OAuth2 사용자 정보 저장/업데이트
     * 회원 가입시 사용
     */
    @Transactional
    public User saveOrUpdateOAuth2User(User user) {
        return userRepository.findByProviderAndProviderId(user.getProvider(), user.getProviderId())
            .map(existingUser -> {
                // 기존 사용자 정보 업데이트
                existingUser.setEmail(user.getEmail());
                existingUser.setName(user.getName());
                existingUser.setProfileImageUrl(user.getProfileImageUrl());
                // Grade는 Optional 패턴으로 안전하게 처리
                user.getGradeOptional().ifPresent(existingUser::setGrade);
                // Role은 기존 사용자의 것을 유지 (변경하지 않음)
                
                log.info("OAuth2 user updated: {}", existingUser.getEmail());
                return userRepository.save(existingUser);
            })
            .orElseGet(() -> {
                // 새 사용자 저장 - Role이 설정되지 않은 경우 기본값 USER로 설정
                if (user.getRole() == null) {
                    user.setRole(UserRole.USER);
                }
                User savedUser = userRepository.save(user);
                log.info("OAuth2 user created: {}", savedUser.getEmail());
                return savedUser;
            });
    }

    /**
     * 회원가입 완료 처리
     */
    @Transactional
    public SignUpResponse completeSignup(SignupRequest signupRequest, HttpServletRequest request, HttpServletResponse response) {
        // Refresh Token으로 사용자 조회
        User user = getUserFromRefreshToken(request);
        
        // 핸드폰 번호 검증
        if (!PhoneNumberValidator.isValid(signupRequest.phoneNumber())) {
            throw new IllegalArgumentException("올바른 핸드폰 번호 형식이 아닙니다");
        }
        
        // 생일 검증 (미래 날짜 불가)
        if (signupRequest.birthDate().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("생일은 미래 날짜가 될 수 없습니다");
        }
        
        // 사용자 정보 업데이트
        user.setPhoneNumber(PhoneNumberValidator.format(signupRequest.phoneNumber()));
        user.setBirthDate(signupRequest.birthDate());
        user.setAddress(signupRequest.address());
        
        User updatedUser = userRepository.save(user);
        
        String newAccessToken = jwtTokenProvider.createAccessToken(updatedUser);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(updatedUser);
        
        // 새로운 Refresh Token을 Cookie에 설정
        Cookie refreshTokenCookie = cookieUtil.createRefreshTokenCookie(newRefreshToken);
        response.addCookie(refreshTokenCookie);
        
        log.info("User signup completed successfully: {}", user.getEmail());
        
        return new SignUpResponse(newAccessToken, UserResponse.of(updatedUser));
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
