package com.homesweet.homesweetback.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.security.jwt.JwtTokenProvider;
import com.homesweet.homesweetback.common.util.CookieUtil;
import com.homesweet.homesweetback.domain.auth.dto.AccessTokenResponse;
import com.homesweet.homesweetback.domain.auth.dto.SignUpResponse;
import com.homesweet.homesweetback.domain.auth.dto.SignupRequest;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.RefreshTokenRepository;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("refreshToken() 메서드 테스트_성공")
    void testRefreshToken_Success() {
        // given
        Long userId = 1L;
        String refreshToken = "valid-refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        User user = createTestUser(userId, "test@test.com", UserRole.USER);
        Cookie cookie = new Cookie("refresh_token", refreshToken);

        given(cookieUtil.getRefreshTokenFromCookie(request)).willReturn(refreshToken);
        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.isRefreshToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(refreshTokenRepository.findByEmail(user.getEmail())).willReturn(refreshToken);
        given(jwtTokenProvider.createAccessToken(user)).willReturn(newAccessToken);
        given(jwtTokenProvider.createRefreshToken(user)).willReturn(newRefreshToken);
        given(refreshTokenRepository.save(user.getEmail(), newRefreshToken)).willReturn(true);
        given(cookieUtil.createRefreshTokenCookie(newRefreshToken)).willReturn(cookie);

        // when
        AccessTokenResponse result = authService.refreshToken(request, response);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo(newAccessToken);
        assertThat(result.userResponse()).isNotNull();
        assertThat(result.userResponse().id()).isEqualTo(userId);
        verify(cookieUtil, times(1)).getRefreshTokenFromCookie(request);
        verify(jwtTokenProvider, times(1)).validateToken(refreshToken);
        verify(jwtTokenProvider, times(1)).isRefreshToken(refreshToken);
        verify(jwtTokenProvider, times(1)).getUserIdFromToken(refreshToken);
        verify(userRepository, times(1)).findById(userId);
        verify(refreshTokenRepository, times(1)).findByEmail(user.getEmail());
        verify(jwtTokenProvider, times(1)).createAccessToken(user);
        verify(jwtTokenProvider, times(1)).createRefreshToken(user);
        verify(refreshTokenRepository, times(1)).save(user.getEmail(), newRefreshToken);
        verify(cookieUtil, times(1)).createRefreshTokenCookie(newRefreshToken);
        verify(response, times(1)).addCookie(cookie);
    }

    @Test
    @DisplayName("refreshToken() 메서드 테스트_실패_RefreshToken 없음")
    void testRefreshToken_Fail_RefreshTokenNotFound() {
        // given
        given(cookieUtil.getRefreshTokenFromCookie(request)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request, response))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Refresh Token을 찾을 수 없습니다");
        verify(cookieUtil, times(1)).getRefreshTokenFromCookie(request);
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("refreshToken() 메서드 테스트_실패_유효하지 않은 RefreshToken")
    void testRefreshToken_Fail_InvalidRefreshToken() {
        // given
        String refreshToken = "invalid-refresh-token";
        Cookie deleteCookie = new Cookie("refresh_token", "");
        given(cookieUtil.getRefreshTokenFromCookie(request)).willReturn(refreshToken);
        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(false);
        given(cookieUtil.createRefreshTokenCookieForDeletion()).willReturn(deleteCookie);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request, response))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은 Refresh Token입니다");
        verify(cookieUtil, times(1)).getRefreshTokenFromCookie(request);
        verify(jwtTokenProvider, times(1)).validateToken(refreshToken);
        verify(jwtTokenProvider, never()).isRefreshToken(anyString());
        verify(cookieUtil, times(1)).createRefreshTokenCookieForDeletion();
        verify(response, times(1)).addCookie(deleteCookie);
    }

    @Test
    @DisplayName("refreshToken() 메서드 테스트_실패_RefreshToken 타입 아님")
    void testRefreshToken_Fail_NotRefreshTokenType() {
        // given
        String refreshToken = "access-token";
        Cookie deleteCookie = new Cookie("refresh_token", "");
        given(cookieUtil.getRefreshTokenFromCookie(request)).willReturn(refreshToken);
        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.isRefreshToken(refreshToken)).willReturn(false);
        given(cookieUtil.createRefreshTokenCookieForDeletion()).willReturn(deleteCookie);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request, response))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은 Refresh Token입니다");
        verify(cookieUtil, times(1)).getRefreshTokenFromCookie(request);
        verify(jwtTokenProvider, times(1)).validateToken(refreshToken);
        verify(jwtTokenProvider, times(1)).isRefreshToken(refreshToken);
        verify(cookieUtil, times(1)).createRefreshTokenCookieForDeletion();
        verify(response, times(1)).addCookie(deleteCookie);
    }

    @Test
    @DisplayName("refreshToken() 메서드 테스트_실패_사용자 없음")
    void testRefreshToken_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        String refreshToken = "valid-refresh-token";
        given(cookieUtil.getRefreshTokenFromCookie(request)).willReturn(refreshToken);
        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.isRefreshToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request, response))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("해당하는 사용자를 찾을 수 없습니다");
        verify(userRepository, times(1)).findById(userId);
        verify(cookieUtil, never()).createRefreshTokenCookieForDeletion();
    }

    @Test
    @DisplayName("refreshToken() 메서드 테스트_실패_저장된 RefreshToken과 불일치")
    void testRefreshToken_Fail_StoredTokenMismatch() {
        // given
        Long userId = 1L;
        String refreshToken = "valid-refresh-token";
        String storedToken = "different-token";
        User user = createTestUser(userId, "test@test.com", UserRole.USER);
        given(cookieUtil.getRefreshTokenFromCookie(request)).willReturn(refreshToken);
        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.isRefreshToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(refreshTokenRepository.findByEmail(user.getEmail())).willReturn(storedToken);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request, response))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은 Refresh Token입니다");
        verify(refreshTokenRepository, times(1)).findByEmail(user.getEmail());
        verify(cookieUtil, never()).createRefreshTokenCookieForDeletion();
    }


    @Test
    @DisplayName("saveOrUpdateOAuth2User() 메서드 테스트_성공_새 사용자 저장")
    void testSaveOrUpdateOAuth2User_Success_NewUser() {
        // given
        User newUser = createTestUser(null, "new@test.com", null);
        newUser.setProvider(OAuth2Provider.GOOGLE);
        newUser.setProviderId("google-123");
        User savedUser = createTestUser(1L, "new@test.com", UserRole.USER);
        savedUser.setProvider(OAuth2Provider.GOOGLE);
        savedUser.setProviderId("google-123");

        given(userRepository.findByProviderAndProviderId(OAuth2Provider.GOOGLE, "google-123"))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        User result = authService.saveOrUpdateOAuth2User(newUser);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("new@test.com");
        assertThat(result.getRole()).isEqualTo(UserRole.USER);
        verify(userRepository, times(1)).findByProviderAndProviderId(OAuth2Provider.GOOGLE, "google-123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("saveOrUpdateOAuth2User() 메서드 테스트_성공_기존 사용자 업데이트")
    void testSaveOrUpdateOAuth2User_Success_UpdateExistingUser() {
        // given
        Long userId = 1L;
        User existingUser = createTestUser(userId, "old@test.com", UserRole.USER);
        existingUser.setProvider(OAuth2Provider.GOOGLE);
        existingUser.setProviderId("google-123");
        existingUser.setName("Old Name");
        existingUser.setProfileImageUrl("https://old-image.com/image.jpg");

        User updatedUser = createTestUser(userId, "new@test.com", UserRole.USER);
        updatedUser.setProvider(OAuth2Provider.GOOGLE);
        updatedUser.setProviderId("google-123");
        updatedUser.setName("New Name");
        updatedUser.setProfileImageUrl("https://new-image.com/image.jpg");

        given(userRepository.findByProviderAndProviderId(OAuth2Provider.GOOGLE, "google-123"))
                .willReturn(Optional.of(existingUser));
        given(userRepository.save(existingUser)).willReturn(existingUser);

        // when
        User result = authService.saveOrUpdateOAuth2User(updatedUser);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("new@test.com");
        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getProfileImageUrl()).isEqualTo("https://old-image.com/image.jpg"); // 기존 이미지 유지
        verify(userRepository, times(1)).findByProviderAndProviderId(OAuth2Provider.GOOGLE, "google-123");
        verify(userRepository, times(1)).save(existingUser);
    }

    @Test
    @DisplayName("completeSignup() 메서드 테스트_성공")
    void testCompleteSignup_Success() {
        // given
        Long userId = 1L;
        String refreshToken = "valid-refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        User user = createTestUser(userId, "test@test.com", UserRole.USER);
        SignupRequest signupRequest = new SignupRequest(
                "010-1234-5678",
                LocalDate.of(1990, 1, 1),
                "서울시 강남구"
        );
        Cookie cookie = new Cookie("refresh_token", newRefreshToken);

        given(cookieUtil.getRefreshTokenFromCookie(request)).willReturn(refreshToken);
        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.isRefreshToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willReturn(user);
        given(jwtTokenProvider.createAccessToken(user)).willReturn(newAccessToken);
        given(jwtTokenProvider.createRefreshToken(user)).willReturn(newRefreshToken);
        given(refreshTokenRepository.save(user.getEmail(), newRefreshToken)).willReturn(true);
        given(cookieUtil.createRefreshTokenCookie(newRefreshToken)).willReturn(cookie);

        // when
        SignUpResponse result = authService.completeSignup(signupRequest, request, response);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo(newAccessToken);
        assertThat(result.userResponse()).isNotNull();
        assertThat(result.userResponse().id()).isEqualTo(userId);
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtTokenProvider, times(1)).createAccessToken(user);
        verify(jwtTokenProvider, times(1)).createRefreshToken(user);
        verify(refreshTokenRepository, times(1)).save(user.getEmail(), newRefreshToken);
        verify(cookieUtil, times(1)).createRefreshTokenCookie(newRefreshToken);
        verify(response, times(1)).addCookie(cookie);
    }

    @Test
    @DisplayName("completeSignup() 메서드 테스트_실패_생일이 미래 날짜")
    void testCompleteSignup_Fail_FutureBirthDate() {
        // given
        Long userId = 1L;
        String refreshToken = "valid-refresh-token";
        User user = createTestUser(userId, "test@test.com", UserRole.USER);
        SignupRequest signupRequest = new SignupRequest(
                "010-1234-5678",
                LocalDate.now().plusDays(1), // 미래 날짜
                "서울시 강남구"
        );

        given(cookieUtil.getRefreshTokenFromCookie(request)).willReturn(refreshToken);
        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.isRefreshToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> authService.completeSignup(signupRequest, request, response))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("생일은 미래 날짜가 될 수 없습니다")
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_BIRTH_DATE);
        verify(userRepository, never()).save(any(User.class));
    }
    

    @Test
    @DisplayName("logout() 메서드 테스트_성공")
    void testLogout_Success() {
        // given
        String accessToken = "valid-access-token";
        Long userId = 1L;
        String email = "test@test.com";
        Cookie deleteCookie = new Cookie("refresh_token", "");

        given(request.getHeader("Authorization")).willReturn("Bearer " + accessToken);
        given(jwtTokenProvider.validateToken(accessToken)).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken(accessToken)).willReturn(userId);
        given(jwtTokenProvider.getEmailFromToken(accessToken)).willReturn(email);
        willDoNothing().given(refreshTokenRepository).deleteByEmail(email);
        given(cookieUtil.createRefreshTokenCookieForDeletion()).willReturn(deleteCookie);

        // when
        authService.logout(request, response);

        // then
        verify(request, times(1)).getHeader("Authorization");
        verify(jwtTokenProvider, times(1)).validateToken(accessToken);
        verify(jwtTokenProvider, times(1)).getUserIdFromToken(accessToken);
        verify(jwtTokenProvider, times(1)).getEmailFromToken(accessToken);
        verify(refreshTokenRepository, times(1)).deleteByEmail(email);
        verify(cookieUtil, times(1)).createRefreshTokenCookieForDeletion();
        verify(response, times(1)).addCookie(deleteCookie);
    }

    @Test
    @DisplayName("logout() 메서드 테스트_성공_Authorization 헤더 없음")
    void testLogout_Success_NoAuthorizationHeader() {
        // given
        Cookie deleteCookie = new Cookie("refresh_token", "");
        given(request.getHeader("Authorization")).willReturn(null);
        given(cookieUtil.createRefreshTokenCookieForDeletion()).willReturn(deleteCookie);

        // when
        authService.logout(request, response);

        // then
        verify(request, times(1)).getHeader("Authorization");
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(refreshTokenRepository, never()).deleteByEmail(anyString());
        verify(cookieUtil, times(1)).createRefreshTokenCookieForDeletion();
        verify(response, times(1)).addCookie(deleteCookie);
    }

    @Test
    @DisplayName("logout() 메서드 테스트_성공_유효하지 않은 AccessToken")
    void testLogout_Success_InvalidAccessToken() {
        // given
        String accessToken = "invalid-access-token";
        Cookie deleteCookie = new Cookie("refresh_token", "");
        given(request.getHeader("Authorization")).willReturn("Bearer " + accessToken);
        given(jwtTokenProvider.validateToken(accessToken)).willReturn(false);
        given(cookieUtil.createRefreshTokenCookieForDeletion()).willReturn(deleteCookie);

        // when
        authService.logout(request, response);

        // then
        verify(request, times(1)).getHeader("Authorization");
        verify(jwtTokenProvider, times(1)).validateToken(accessToken);
        verify(refreshTokenRepository, never()).deleteByEmail(anyString());
        verify(cookieUtil, times(1)).createRefreshTokenCookieForDeletion();
        verify(response, times(1)).addCookie(deleteCookie);
    }

    private User createTestUser(Long userId, String email, UserRole role) {
        User user = User.builder()
                .email(email)
                .name("test")
                .role(role != null ? role : UserRole.USER)
                .provider(OAuth2Provider.GOOGLE)
                .providerId("123456789")
                .build();
        if (userId != null) {
            user.setId(userId);
        }
        return user;
    }
}
