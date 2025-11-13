package com.homesweet.homesweetback.domain.auth.service;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.security.jwt.JwtTokenProvider;
import com.homesweet.homesweetback.common.util.CookieUtil;
import com.homesweet.homesweetback.domain.auth.dto.*;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.RefreshTokenRepository;
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
    private final RefreshTokenRepository refreshTokenRepository;
    /**
     * Refresh Token으로 새로운 Access Token을 발급합니다.
     */
    @Transactional
    public AccessTokenResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // Cookie에서 Refresh Token 추출
        String refreshToken = getRefreshTokenFromRequest(request);

        // Refresh Token 유효성 검증
        validateRefreshToken(response, refreshToken);

        // Refresh Token으로 사용자 조회
        User user = getUserById(jwtTokenProvider.getUserIdFromToken(refreshToken));

        // 조회한 사용자의 email로 저장된 토큰 확인
        validateStoredRefreshToken(refreshToken, user);

        // 새로운 Access Token과 Refresh Token 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(user);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user);
        refreshTokenRepository.save(user.getEmail(), newRefreshToken);

        // 새로운 Refresh Token을 Cookie에 설정
        Cookie refreshTokenCookie = cookieUtil.createRefreshTokenCookie(newRefreshToken);
        response.addCookie(refreshTokenCookie);

        log.info("Token refreshed successfully for user: {}", user.getEmail());

        return new AccessTokenResponse(newAccessToken,UserResponse.of(user));
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
                if (existingUser.getProfileImageUrl() == null) {
                    existingUser.setProfileImageUrl(user.getProfileImageUrl());
                }
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
        
        // 생일 검증 (미래 날짜 불가)
        if (signupRequest.birthDate().isAfter(java.time.LocalDate.now())) {
            throw new BusinessException(ErrorCode.INVALID_BIRTH_DATE);
        }
        
        // 사용자 정보 업데이트
        user.setPhoneNumber(PhoneNumberValidator.format(signupRequest.phoneNumber()));
        user.setBirthDate(signupRequest.birthDate());
        user.setAddress(signupRequest.address());
        
        User updatedUser = userRepository.save(user);
        
        String newAccessToken = jwtTokenProvider.createAccessToken(updatedUser);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(updatedUser);
        
        // 새로운 Refresh Token을 Redis에 저장
        refreshTokenRepository.save(user.getEmail(), newRefreshToken);
        
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
        String accessToken = getAccessTokenFromRequest(request);

        if (jwtTokenProvider.validateToken(accessToken)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);
            String email = jwtTokenProvider.getEmailFromToken(accessToken);
                
            log.info("User logout successful: userId={}, email={}", userId, email);
            refreshTokenRepository.deleteByEmail(email);
        }
        
        // Refresh Token Cookie 삭제
        Cookie deleteCookie = cookieUtil.createRefreshTokenCookieForDeletion();
        response.addCookie(deleteCookie);
    }

    // 내부 메서드

    /**
     * Authorization 헤더에서 Access Token을 추출합니다.
     */
    private String getAccessTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Cookie에서 Refresh Token을 추출합니다.
     */
    private String getRefreshTokenFromRequest(HttpServletRequest request) {
        String refreshToken = cookieUtil.getRefreshTokenFromCookie(request);

        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        return refreshToken;
    }


    /**
     * Refresh Token 유효성 검증
     */
    private void validateRefreshToken(HttpServletResponse response, String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            // 유효하지 않은 refresh token인 경우 cookie 삭제
            Cookie deleteCookie = cookieUtil.createRefreshTokenCookieForDeletion();
            response.addCookie(deleteCookie);
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    /**
     * User ID로 사용자 정보를 조회합니다.
     */
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }


    /**
     * Refresh Token으로 사용자 정보를 조회합니다.
     */
    private User getUserFromRefreshToken(HttpServletRequest request) {
        String refreshToken = cookieUtil.getRefreshTokenFromCookie(request);
        
        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        return getUserById(userId);
    }

    /**
     * 저장된 Refresh Token과 비교하여 유효성 검증
     */
    private void validateStoredRefreshToken(String refreshToken, User user) {
        String storedRefreshToken = refreshTokenRepository.findByEmail(user.getEmail());
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

}
