package com.homesweet.homesweetback.domain.auth.entity;

/**
 * OAuth2 Provider 열거형
 * 지원하는 OAuth2 제공업체를 정의합니다.
 * 이 서비스는 OAuth2만 지원하며 로컬 로그인은 제공하지 않습니다.
 */
public enum OAuth2Provider {
    /**
     * Google OAuth2
     */
    GOOGLE("google"),
    
    /**
     * Kakao OAuth2 (향후 지원 예정)
     */
    KAKAO("kakao");
    

    private final String providerName;

    OAuth2Provider(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderName() {
        return providerName;
    }

    /**
     * Provider 이름으로 enum을 찾습니다.
     * 
     * @param providerName Provider 이름
     * @return 해당하는 OAuth2Provider enum
     * @throws IllegalArgumentException 지원하지 않는 Provider인 경우
     */
    public static OAuth2Provider fromProviderName(String providerName) {
        for (OAuth2Provider provider : values()) {
            if (provider.providerName.equals(providerName)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unsupported OAuth2 provider: " + providerName);
    }
}
