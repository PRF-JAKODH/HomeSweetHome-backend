package com.homesweet.homesweetback.domain.auth.util;   

import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kakao OAuth2 사용자 정보 추출기 (향후 구현 예정)
 */
@Component
public class KakaoUserInfoExtractor implements OAuth2UserInfoExtractor {
    
    @Override
    public OAuth2Provider getSupportedProvider() {
        return OAuth2Provider.KAKAO;
    }
    
    @Override
    public String extractUserId(Map<String, Object> attributes) {
        // Kakao의 사용자 ID는 숫자형일 수 있으므로 String으로 변환
        Object id = attributes.get("id");
        return id != null ? id.toString() : null;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public String extractEmail(Map<String, Object> attributes) {
        // Kakao는 계정 정보가 중첩된 구조로 제공됨
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        return account != null ? (String) account.get("email") : null;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public String extractName(Map<String, Object> attributes) {
        // Kakao는 프로필 정보가 중첩된 구조로 제공됨
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        if (account != null) {
            Map<String, Object> profile = (Map<String, Object>) account.get("profile");
            return profile != null ? (String) profile.get("nickname") : null;
        }
        return null;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public String extractProfileImageUrl(Map<String, Object> attributes) {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        if (account != null) {
            Map<String, Object> profile = (Map<String, Object>) account.get("profile");
            return profile != null ? (String) profile.get("profile_image_url") : null;
        }
        return null;
    }
}
