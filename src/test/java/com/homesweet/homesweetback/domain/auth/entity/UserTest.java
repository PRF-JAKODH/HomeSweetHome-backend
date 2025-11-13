package com.homesweet.homesweetback.domain.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.homesweet.homesweetback.domain.grade.entity.Grade;

@DisplayName("User 엔티티 테스트")
class UserTest {

    @Test
    @DisplayName("isOAuthUser() 메서드 테스트_항상 true 반환")
    void testIsOAuthUser_AlwaysTrue() {
        // given
        User user = createTestUser();

        // when
        boolean isOAuthUser = user.isOAuthUser();

        // then
        assertThat(isOAuthUser).isTrue();
    }

    @Test
    @DisplayName("isSameProvider() 메서드 테스트_성공_같은 Provider")
    void testIsSameProvider_Success_SameProvider() {
        // given
        User user = createTestUser();
        OAuth2Provider provider = OAuth2Provider.GOOGLE;

        // when
        boolean isSameProvider = user.isSameProvider(provider);

        // then
        assertThat(isSameProvider).isTrue();
    }

    @Test
    @DisplayName("isSameProvider() 메서드 테스트_실패_다른 Provider")
    void testIsSameProvider_Fail_DifferentProvider() {
        // given
        User user = User.builder()
                .email("test@test.com")
                .name("test")
                .role(UserRole.USER)
                .provider(OAuth2Provider.GOOGLE)
                .providerId("123456789")
                .build();

        // whe
        boolean isSameProvider = user.isSameProvider(OAuth2Provider.KAKAO);

        // then
        assertThat(isSameProvider).isFalse();
    }

    @Test
    @DisplayName("getGradeOptional() 메서드 테스트_성공_등급이 있는 경우")
    void testGetGradeOptional_Success_WithGrade() {
        // given
        Grade grade = Grade.builder()
                .grade("GOLD")
                .feeRate(new BigDecimal("5.00"))
                .build();
        User user = User.builder()
                .email("test@test.com")
                .name("test")
                .role(UserRole.USER)
                .provider(OAuth2Provider.GOOGLE)
                .providerId("123456789")
                .grade(grade)
                .build();

        // when
        Optional<Grade> gradeOptional = user.getGradeOptional();

        // then
        assertThat(gradeOptional).isPresent();
        assertThat(gradeOptional.get()).isEqualTo(grade);
        assertThat(gradeOptional.get().getGrade()).isEqualTo("GOLD");
    }

    @Test
    @DisplayName("getGradeOptional() 메서드 테스트_성공_등급이 없는 경우")
    void testGetGradeOptional_Success_WithoutGrade() {
        // given
        User user = createTestUser();

        // when
        Optional<Grade> gradeOptional = user.getGradeOptional();

        // then
        assertThat(gradeOptional).isEmpty();
    }

    @Test
    @DisplayName("hasGrade() 메서드 테스트_성공_등급이 있는 경우")
    void testHasGrade_Success_WithGrade() {
        // given
        Grade grade = Grade.builder()
                .grade("GOLD")
                .feeRate(new BigDecimal("5.00"))
                .build();
        User user = User.builder()
                .email("test@test.com")
                .name("test")
                .role(UserRole.USER)
                .provider(OAuth2Provider.GOOGLE)
                .providerId("123456789")
                .grade(grade)
                .build();

        // when
        boolean hasGrade = user.hasGrade();

        // then
        assertThat(hasGrade).isTrue();
    }

    @Test
    @DisplayName("hasGrade() 메서드 테스트_성공_등급이 없는 경우")
    void testHasGrade_Success_WithoutGrade() {
        // given
        User user = createTestUser();

        // when
        boolean hasGrade = user.hasGrade();

        // then
        assertThat(hasGrade).isFalse();
    }

    @Test
    @DisplayName("getGradeName() 메서드 테스트_성공_등급이 있는 경우")
    void testGetGradeName_Success_WithGrade() {
        // given
        Grade grade = Grade.builder()
                .grade("GOLD")
                .feeRate(new BigDecimal("5.00"))
                .build();
        User user = User.builder()
                .email("test@test.com")
                .name("test")
                .role(UserRole.USER)
                .provider(OAuth2Provider.GOOGLE)
                .providerId("123456789")
                .grade(grade)
                .build();

        // when
        String gradeName = user.getGradeName();

        // then
        assertThat(gradeName).isEqualTo("GOLD");
    }

    @Test
    @DisplayName("getGradeName() 메서드 테스트_성공_등급이 없는 경우")
    void testGetGradeName_Success_WithoutGrade() {
        // given
        User user = createTestUser();

        // when
        String gradeName = user.getGradeName();

        // then
        assertThat(gradeName).isEqualTo("등급 없음");
    }

    @Test
    @DisplayName("getFeeRate() 메서드 테스트_성공_등급이 있는 경우")
    void testGetFeeRate_Success_WithGrade() {
        // given
        BigDecimal feeRate = new BigDecimal("5.00");
        Grade grade = Grade.builder()
                .grade("GOLD")
                .feeRate(feeRate)
                .build();
        User user = User.builder()
                .email("test@test.com")
                .name("test")
                .role(UserRole.USER)
                .provider(OAuth2Provider.GOOGLE)
                .providerId("123456789")
                .grade(grade)
                .build();

        // when
        BigDecimal userFeeRate = user.getFeeRate();

        // then
        assertThat(userFeeRate).isEqualByComparingTo(feeRate);
    }

    @Test
    @DisplayName("getFeeRate() 메서드 테스트_성공_등급이 없는 경우")
    void testGetFeeRate_Success_WithoutGrade() {
        // given
        User user = createTestUser();

        // when
        BigDecimal feeRate = user.getFeeRate();

        // then
        assertThat(feeRate).isEqualByComparingTo(BigDecimal.ZERO);
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

