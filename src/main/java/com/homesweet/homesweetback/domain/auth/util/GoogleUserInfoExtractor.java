package com.homesweet.homesweetback.domain.auth.util;    

import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Google OAuth2 사용자 정보 추출기
 */
@Component
public class GoogleUserInfoExtractor implements OAuth2UserInfoExtractor {
    
    @Override
    public OAuth2Provider getSupportedProvider() {
        return OAuth2Provider.GOOGLE;
    }
    
    @Override
    public String extractUserId(Map<String, Object> attributes) {
        return (String) attributes.get("id");
    }
    
    @Override
    public String extractEmail(Map<String, Object> attributes) {
        return (String) attributes.get("email");
    }
    
    @Override
    public String extractName(Map<String, Object> attributes) {
        return (String) attributes.get("name");
    }
    
    @Override
    public String extractProfileImageUrl(Map<String, Object> attributes) {
        return (String) attributes.get("picture");
    }
}
