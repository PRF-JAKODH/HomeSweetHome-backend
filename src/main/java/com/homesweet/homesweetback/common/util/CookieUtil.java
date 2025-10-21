package com.homesweet.homesweetback.common.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cookie 관련 유틸리티 클래스
 */
@Slf4j
@Component
public class CookieUtil {

    @Value("${cookie.secure:false}") 
    private boolean cookieSecure;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * Cookie에서 Refresh Token을 추출합니다.
     */
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 특정 이름의 Cookie 값을 추출합니다.
     */
    public String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Refresh Token용 Cookie 삭제
     */
    public Cookie createRefreshTokenCookieForDeletion() {
        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0); // 즉시 삭제
        cookie.setAttribute("SameSite", "Strict");
        return cookie;
    }

            /**
     * Refresh Token용 HttpOnly Cookie 생성
     */
    public Cookie createRefreshTokenCookie(String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge((int) (refreshTokenExpiration / 1000)); // 초 단위로 변환
        cookie.setAttribute("SameSite", "Strict");
        return cookie;
    }
}
