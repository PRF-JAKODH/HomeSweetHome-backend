package com.homesweet.homesweetback.common.security.jwt;


import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * JWT 토큰을 검증하고 인증 정보를 설정하는 필터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, 
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
        var path = request.getRequestURI();
        log.debug("{}",path);
        if("/ws".equals(path)) {
            log.debug("passed {}",path);
            filterChain.doFilter(request, response);
            return;
        }
        try {
            // Access Token을 Authorization 헤더에서 추출
            String accessToken = getJwtFromRequest(request);
            if (StringUtils.hasText(accessToken)) {
                /**
                 * 테스트 용 test 유저 처리
                 */
                // 테스트용: 1~10의 토큰으로 해당 user_id의 사용자를 인증
                if (isTestToken(accessToken)) {
                    Long userId = Long.parseLong(accessToken);
                    if (userId >= 1 && userId <= 10) {
                        User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("Test user not found with id: " + userId));

                        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(user, null);

                        Collection<GrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority(user.getRole().getAuthority())
                        );
                        
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        log.info("TEST MODE: Authenticated user with test token. User ID: {}, Email: {}", userId, user.getEmail());
                    }
                } else if (jwtTokenProvider.validateToken(accessToken)) {
                    // Access Token인지 확인 (Refresh Token이 아닌지)
                    if (!jwtTokenProvider.isRefreshToken(accessToken)) {
                        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);
                        
                        User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

                        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(user, null);

                        // 사용자의 Role을 authorities로 설정
                        Collection<GrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority(user.getRole().getAuthority())
                        );
                        
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        log.debug("JWT authentication successful for user: {}", user.getEmail());
                    } else {
                        log.warn("Access token required, but refresh token provided in Authorization header");
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청에서 JWT 토큰을 추출합니다.
     * Authorization 헤더에서 "Bearer " 접두사를 제거합니다.
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }

    /**
     * 테스트용 토큰인지 확인합니다.
     * 1~10 사이의 숫자인 경우 테스트용 토큰으로 간주합니다.
     * 
     * 주의: 이 기능은 테스트 목적으로만 사용하세요. 실제 배포 환경에서는 제거해야 합니다.
     */
    private boolean isTestToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        try {
            // 숫자인지 확인하고 1~10 범위인지 체크
            Long.parseLong(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
