package com.homesweet.homesweetback.common.security.jwt;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;

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
        this.secretKey = new SecretKeySpec(secret.getBytes(), "HMACSHA256");
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
            .setSubject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("name", user.getName())
            .claim("provider", user.getProvider().getProviderName())
            .claim("role", user.getRole().name())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS256, secretKey.getEncoded())
            .compact();
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
            .setSubject(user.getId().toString())
            .claim("type", "refresh")
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS256, secretKey.getEncoded())
            .compact();
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(secretKey)
            .parseClaimsJws(token)
            .getBody();

        return Long.parseLong(claims.getSubject());
    }

    /**
     * 토큰에서 이메일 추출
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(secretKey.getEncoded())
            .parseClaimsJws(token)
            .getBody();

        return claims.get("email", String.class);
    }

    /**
     * 토큰에서 역할(Role) 추출
     */
    public UserRole getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(secretKey.getEncoded())
            .parseClaimsJws(token)
            .getBody();

        String roleName = claims.get("role", String.class);
        return UserRole.valueOf(roleName);
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
            return true;
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰이 만료되었는지 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
            
            return claims.getExpiration().before(new Date()) || claims.getExpiration().equals(new Date());
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return true;
        }
    }

        /**
     * 토큰 타입 확인 (access token인지 refresh token인지)
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(secretKey.getEncoded())
                .parseClaimsJws(token)
                .getBody();
            
            String type = claims.get("type", String.class);
            return "refresh".equals(type);
            } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}
