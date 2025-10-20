package com.homesweet.homesweetback.common.security.oauth;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.service.UserService;
import com.homesweet.homesweetback.domain.auth.util.OAuth2UserInfoExtractor;
import com.homesweet.homesweetback.domain.auth.util.OAuth2UserInfoExtractorFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Random;

/**
 * OAuth2 사용자 정보를 처리하는 커스텀 서비스
 * Google OAuth2에서 받은 사용자 정보를 처리하고 DB에 저장/업데이트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;
    private final OAuth2UserInfoExtractorFactory extractorFactory;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String provider = userRequest.getClientRegistration().getRegistrationId();
        log.info("OAuth2 login attempt with provider: {}", provider);
        
        try {
            return processOAuth2User(provider, oAuth2User);
        } catch (Exception e) {
            log.error("Error processing OAuth2 user: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException("OAuth2 user processing failed");
        }
    }

    private OAuth2User processOAuth2User(String providerName, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        // Provider에 따른 사용자 정보 추출기 선택
        OAuth2Provider provider;
        try {
            provider = OAuth2Provider.valueOf(providerName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + providerName);
        }
        
        OAuth2UserInfoExtractor extractor = extractorFactory.getExtractor(provider);
        
        // 사용자 정보 추출
        String providerId = extractor.extractUserId(attributes);
        String email = extractor.extractEmail(attributes);
        String name = extractor.extractName(attributes);
        String profileImageUrl = extractor.extractProfileImageUrl(attributes);

        log.info("providerId: {}", providerId);
        log.info("email: {}", email);
        log.info("name: {}", name);
        log.info("profileImageUrl: {}", profileImageUrl);

        // 필수 정보 검증
        if (!extractor.validateRequiredFields(attributes)) {
            throw new OAuth2AuthenticationException("Missing required user information from " + provider);
        }

        // 랜덤 등금 생성
        String grade = switch (new Random().nextInt(0, 4)) {
            case 0 -> "VVIP";
            case 1 -> "VIP";
            case 2 -> "GOLD";
            case 3 -> "SILVER";
            default -> throw new IllegalStateException("Unexpected value");
        };
        
        // UserService를 통해 사용자 저장/업데이트
        User user = User.builder()
            .provider(provider)
            .providerId(providerId)
            .email(email)
            .name(name)
            .profileImageUrl(profileImageUrl)
            .grade(grade) // 등급 랜덤 생성
            .build();
        
        user = userService.saveOrUpdateOAuth2User(user);

        log.info("OAuth2 login successful for user: {} (provider: {})", email, provider);
        return new OAuth2UserPrincipal(user, attributes);
    }
}
