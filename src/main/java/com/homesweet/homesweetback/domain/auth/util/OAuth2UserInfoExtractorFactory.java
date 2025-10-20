package com.homesweet.homesweetback.domain.auth.util;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OAuth2 Provider별 사용자 정보 추출기를 관리하는 팩토리 클래스
 */
@Slf4j
@Component
public class OAuth2UserInfoExtractorFactory {
    
    private final List<OAuth2UserInfoExtractor> extractors;
    private final Map<OAuth2Provider, OAuth2UserInfoExtractor> extractorMap;
    
    public OAuth2UserInfoExtractorFactory(List<OAuth2UserInfoExtractor> extractors) {
        this.extractors = extractors;
        this.extractorMap = extractors.stream()
            .collect(Collectors.toMap(
                OAuth2UserInfoExtractor::getSupportedProvider,
                Function.identity()
            ));
        
        log.info("Registered OAuth2 user info extractors: {}", 
            extractorMap.keySet().stream()
                .map(OAuth2Provider::getProviderName)
                .collect(Collectors.joining(", ")));
    }
    
    /**
     * 지정된 Provider에 대한 사용자 정보 추출기를 반환합니다.
     */
    public OAuth2UserInfoExtractor getExtractor(OAuth2Provider provider) {
        OAuth2UserInfoExtractor extractor = extractorMap.get(provider);
        if (extractor == null) {
            throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        }
        return extractor;
    }
    
    /**
     * Provider 이름으로 사용자 정보 추출기를 반환합니다.
     */
    public OAuth2UserInfoExtractor getExtractor(String providerName) {
        OAuth2Provider provider = OAuth2Provider.valueOf(providerName.toUpperCase());
        return getExtractor(provider);
    }
    
    /**
     * 지원하는 모든 Provider 목록을 반환합니다.
     */
    public List<OAuth2Provider> getSupportedProviders() {
        return extractors.stream()
            .map(OAuth2UserInfoExtractor::getSupportedProvider)
            .collect(Collectors.toList());
    }
}