package com.homesweet.homesweetback.common.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증을 담당하는 클래스
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                           @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
                           @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("name", user.getName())
            .claim("provider", user.getProvider().getProviderName())
            .claim("role", user.getRole().name())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact();
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("type", "refresh")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact();
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserIdFromToken(String token) {
        Jws<Claims> claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token);
        String subject = claims.getPayload().getSubject().toString();
        return Long.parseLong(subject);
    }

    /**
     * 토큰에서 이메일 추출
     */
    public String getEmailFromToken(String token) {
        Jws<Claims> claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token);
            
        return claims.getPayload().get("email", String.class).toString();
    }

    /**
     * 토큰에서 역할(Role) 추출
     */
    public UserRole getRoleFromToken(String token) {
        Jws<Claims> claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token);
        String roleName = claims.getPayload().get("role", String.class).toString();
        return UserRole.valueOf(roleName);
    }

    /**
     * 공통 Claims 파싱 메서드
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰이 만료되었는지 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Jws<Claims> claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return claims.getPayload().getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * 토큰 타입 확인 (access token인지 refresh token인지)
     */
    public boolean isRefreshToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            log.info("claims: {}", claims.getPayload());
            String type = claims.getPayload().get("type", String.class).toString();
            
            return "refresh".equals(type);
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }
}
