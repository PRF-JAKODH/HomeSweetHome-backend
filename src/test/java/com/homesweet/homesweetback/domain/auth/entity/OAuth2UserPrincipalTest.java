package com.homesweet.homesweetback.domain.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

@DisplayName("OAuth2UserPrincipal 테스트")
class OAuth2UserPrincipalTest {

    @Test
    @DisplayName("getAttributes() 메서드 테스트_성공")
    void testGetAttributes_Success() {
        // given
        User user = createTestUser();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "123456789");
        attributes.put("email", "test@test.com");
        attributes.put("name", "Test User");
        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(user, attributes);

        // when
        Map<String, Object> result = principal.getAttributes();

        // then
        assertThat(result).isEqualTo(attributes);
        assertThat(result.get("sub")).isEqualTo("123456789");
        assertThat(result.get("email")).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("getAuthorities() 메서드 테스트_성공_ROLE_USER 반환")
    void testGetAuthorities_Success() {
        // given
        User user = createTestUser();
        Map<String, Object> attributes = new HashMap<>();
        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(user, attributes);

        // when
        var authorities = principal.getAuthorities();

        // then
        assertThat(authorities).isNotEmpty();
        assertThat(authorities.size()).isEqualTo(1);
        GrantedAuthority authority = authorities.iterator().next();
        assertThat(authority.getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("getName() 메서드 테스트_성공_이메일 반환")
    void testGetName_Success() {
        // given
        User user = createTestUser();
        Map<String, Object> attributes = new HashMap<>();
        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(user, attributes);

        // when
        String name = principal.getName();

        // then
        assertThat(name).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("getUserId() 메서드 테스트_성공")
    void testGetUserId_Success() {
        // given
        User user = createTestUser();
        user.setId(1L);
        Map<String, Object> attributes = new HashMap<>();
        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(user, attributes);

        // when
        Long userId = principal.getUserId();

        // then
        assertThat(userId).isEqualTo(1L);
    }

    @Test
    @DisplayName("getEmail() 메서드 테스트_성공")
    void testGetEmail_Success() {
        // given
        User user = createTestUser();
        Map<String, Object> attributes = new HashMap<>();
        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(user, attributes);

        // when
        String email = principal.getEmail();

        // then
        assertThat(email).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("getDisplayName() 메서드 테스트_성공")
    void testGetDisplayName_Success() {
        // given
        User user = createTestUser();
        Map<String, Object> attributes = new HashMap<>();
        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(user, attributes);

        // when
        String displayName = principal.getDisplayName();

        // then
        assertThat(displayName).isEqualTo("test");
    }

    @Test
    @DisplayName("getProvider() 메서드 테스트_성공")
    void testGetProvider_Success() {
        // given
        User user = createTestUser();
        Map<String, Object> attributes = new HashMap<>();
        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(user, attributes);

        // when
        String provider = principal.getProvider();

        // then
        assertThat(provider).isEqualTo("google");
    }

    @Test
    @DisplayName("생성자 테스트_User와 Attributes 저장 확인")
    void testConstructor_Success() {
        // given
        User user = createTestUser();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "123456789");

        // when
        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(user, attributes);

        // then
        assertThat(principal.getUser()).isEqualTo(user);
        assertThat(principal.getAttributes()).isEqualTo(attributes);
    }

    private User createTestUser() {
        return User.builder()
                .email("test@test.com")
                .name("test")
                .role(UserRole.USER)
                .provider(OAuth2Provider.GOOGLE)
                .providerId("123456789")
                .build();
    }
}

