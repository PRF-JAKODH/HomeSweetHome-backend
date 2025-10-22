package com.homesweet.homesweetback.domain.auth.util;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;

import java.util.Map;

/**
 * OAuth2 Provider별 사용자 정보 추출 인터페이스
 */
public interface OAuth2UserInfoExtractor {
    
    /**
     * 이 추출기가 지원하는 OAuth2 Provider를 반환합니다.
     */
    OAuth2Provider getSupportedProvider();
    
    /**
     * OAuth2 사용자 속성에서 사용자 ID를 추출합니다.
     */
    String extractUserId(Map<String, Object> attributes);
    
    /**
     * OAuth2 사용자 속성에서 이메일을 추출합니다.
     */
    String extractEmail(Map<String, Object> attributes);
    
    /**
     * OAuth2 사용자 속성에서 이름을 추출합니다.
     */
    String extractName(Map<String, Object> attributes);
    
    /**
     * OAuth2 사용자 속성에서 프로필 이미지 URL을 추출합니다.
     */
    String extractProfileImageUrl(Map<String, Object> attributes);
    
    /**
     * 필수 정보가 모두 존재하는지 검증합니다.
     */
    default boolean validateRequiredFields(Map<String, Object> attributes) {
        return extractUserId(attributes) != null && 
               extractEmail(attributes) != null && 
               extractName(attributes) != null;
    }
}
