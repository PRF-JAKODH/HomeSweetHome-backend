package com.homesweet.homesweetback.domain.auth.entity;

import lombok.Getter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * OAuth2 사용자 정보를 담는 Principal 클래스
 * Spring Security의 OAuth2User 인터페이스를 구현
 */
@Getter
public class OAuth2UserPrincipal implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;

    public OAuth2UserPrincipal(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 기본적으로 모든 OAuth 사용자는 USER 권한을 가짐
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() {
        return user.getEmail();
    }

    /**
     * 사용자 ID 반환
     */
    public Long getUserId() {
        return user.getId();
    }

    /**
     * 사용자 이메일 반환
     */
    public String getEmail() {
        return user.getEmail();
    }

    /**
     * 사용자 이름 반환
     */
    public String getDisplayName() {
        return user.getName();
    }

    /**
     * OAuth Provider 반환
     */
    public String getProvider() {
        return user.getProvider().getProviderName();
    }
}

