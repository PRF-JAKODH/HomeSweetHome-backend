package com.homesweet.homesweetback.domain.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserRole 테스트")
class UserRoleTest {

    @Test
    @DisplayName("getAuthority() 메서드 테스트_USER")
    void testGetAuthority_User() {
        // when
        String authority = UserRole.USER.getAuthority();

        // then
        assertThat(authority).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("getAuthority() 메서드 테스트_SELLER")
    void testGetAuthority_Seller() {
        // when
        String authority = UserRole.SELLER.getAuthority();

        // then
        assertThat(authority).isEqualTo("ROLE_SELLER");
    }

    @Test
    @DisplayName("fromAuthority() 메서드 테스트_성공_USER")
    void testFromAuthority_Success_User() {
        // when
        UserRole role = UserRole.fromAuthority("ROLE_USER");

        // then
        assertThat(role).isEqualTo(UserRole.USER);
        assertThat(role.getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("fromAuthority() 메서드 테스트_성공_SELLER")
    void testFromAuthority_Success_Seller() {
        // when
        UserRole role = UserRole.fromAuthority("ROLE_SELLER");

        // then
        assertThat(role).isEqualTo(UserRole.SELLER);
        assertThat(role.getAuthority()).isEqualTo("ROLE_SELLER");
    }

    @Test
    @DisplayName("fromAuthority() 메서드 테스트_실패_지원하지 않는 Authority")
    void testFromAuthority_Fail_UnsupportedAuthority() {
        // when
        UserRole role = UserRole.fromAuthority("ROLE_ADMIN");

        // then
        assertThat(role).isNull();
    }

    @Test
    @DisplayName("fromAuthority() 메서드 테스트_실패_null 값")
    void testFromAuthority_Fail_Null() {
        // when
        UserRole role = UserRole.fromAuthority(null);

        // then
        assertThat(role).isNull();
    }

    @Test
    @DisplayName("fromAuthority() 메서드 테스트_실패_빈 문자열")
    void testFromAuthority_Fail_EmptyString() {
        // when
        UserRole role = UserRole.fromAuthority("");

        // then
        assertThat(role).isNull();
    }
}

