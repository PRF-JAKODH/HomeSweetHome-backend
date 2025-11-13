package com.homesweet.homesweetback.domain.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OAuth2Provider 테스트")
class OAuth2ProviderTest {

    @Test
    @DisplayName("getProviderName() 메서드 테스트_성공_GOOGLE")
    void testGetProviderName_Success_Google() {
        // when
        String providerName = OAuth2Provider.GOOGLE.getProviderName();

        // then
        assertThat(providerName).isEqualTo("google");
    }

    @Test
    @DisplayName("getProviderName() 메서드 테스트_성공_KAKAO")
    void testGetProviderName_Success_Kakao() {
        // when
        String providerName = OAuth2Provider.KAKAO.getProviderName();

        // then
        assertThat(providerName).isEqualTo("kakao");
    }

    @Test
    @DisplayName("fromProviderName() 메서드 테스트_성공_GOOGLE")
    void testFromProviderName_Success_Google() {
        // when
        OAuth2Provider provider = OAuth2Provider.fromProviderName("google");

        // then
        assertThat(provider).isEqualTo(OAuth2Provider.GOOGLE);
        assertThat(provider.getProviderName()).isEqualTo("google");
    }

    @Test
    @DisplayName("fromProviderName() 메서드 테스트_성공_KAKAO")
    void testFromProviderName_Success_Kakao() {
        // when
        OAuth2Provider provider = OAuth2Provider.fromProviderName("kakao");

        // then
        assertThat(provider).isEqualTo(OAuth2Provider.KAKAO);
        assertThat(provider.getProviderName()).isEqualTo("kakao");
    }

    @Test
    @DisplayName("fromProviderName() 메서드 테스트_실패_지원하지 않는 Provider")
    void testFromProviderName_Fail_UnsupportedProvider() {
        // when & then
        assertThatThrownBy(() -> OAuth2Provider.fromProviderName("facebook"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported OAuth2 provider: facebook");
    }

    @Test
    @DisplayName("fromProviderName() 메서드 테스트_실패_null 값")
    void testFromProviderName_Fail_Null() {
        // when & then
        assertThatThrownBy(() -> OAuth2Provider.fromProviderName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported OAuth2 provider: null");
    }

    @Test
    @DisplayName("fromProviderName() 메서드 테스트_실패_빈 문자열")
    void testFromProviderName_Fail_EmptyString() {
        // when & then
        assertThatThrownBy(() -> OAuth2Provider.fromProviderName(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported OAuth2 provider: ");
    }
}

