package com.homesweet.homesweetback.common.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2 로그인 실패 시 처리하는 핸들러
 * 에러 메시지를 클라이언트로 리다이렉트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/auth/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                      AuthenticationException exception) throws IOException {
        
        log.error("OAuth2 authentication failed: {}", exception.getMessage(), exception);
        
        // 에러 메시지를 URL 파라미터로 전달
        String errorMessage = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("success", "false")
                .queryParam("error", "authentication_failed")
                .queryParam("message", errorMessage)
                .build()
                .toUriString();
        
        log.info("Redirecting to error page: {}", targetUrl);
        
        // 클라이언트로 리다이렉트
        response.sendRedirect(targetUrl);
        
        log.info("OAuth2 authentication failure redirected to client");
    }
}

