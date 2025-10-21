package com.homesweet.homesweetback.common.security.oauth;

import com.homesweet.homesweetback.common.security.jwt.JwtTokenProvider;
import com.homesweet.homesweetback.common.util.CookieUtil;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 로그인 성공 시 처리하는 핸들러
 * JWT 토큰을 생성하고 클라이언트로 리다이렉트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    private final CookieUtil cookieUtil;
    
    @Value("${app.oauth2.redirect-uri:http://localhost:3000/auth/callback}")
    private String redirectUriOnLogin;

    @Value("${app.oauth2.redirect-uri-on-register:http://localhost:3000/auth/callback}")
    private String redirectUriOnRegister;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {
        
        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        
        log.info("OAuth2 authentication successful for user: {}", principal.getEmail());
        
        // JWT 토큰 생성
        String refreshToken = jwtTokenProvider.createRefreshToken(principal.getUser());
        
        // Refresh Token을 HttpOnly Cookie로 설정
        Cookie refreshTokenCookie = cookieUtil.createRefreshTokenCookie(refreshToken);
        response.addCookie(refreshTokenCookie);
        
        
        // 사용자의 핸드폰 번호가 있는지 확인
        boolean needsSignup = principal.getUser().getPhoneNumber() == null || 
                             principal.getUser().getBirthDate() == null;
        log.info(principal.getUser().getPhoneNumber());
        
        // 로그인 성공 후 리다이렉트 (Access Token을 URL 파라미터로 전달)
        String redirectUrl = UriComponentsBuilder.fromUriString(needsSignup ? redirectUriOnRegister : redirectUriOnLogin)
                .build()
                .toUriString();
        
        log.info("Redirecting to: {}", redirectUrl);
        
        // 클라이언트로 리다이렉트(프론트엔드)
        response.sendRedirect(redirectUrl);
        
        log.info("OAuth2 authentication completed and redirected for user: {}", principal.getEmail());
    }
}