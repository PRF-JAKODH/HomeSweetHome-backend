package com.homesweet.homesweetback.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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

/**
 * AuthService 통합 테스트
 * - 실제 DB(H2)를 사용하여 전체 플로우 검증
 * - 실제 JWT 토큰 생성 및 검증
 * - 트랜잭션 롤백으로 테스트 격리 보장
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthService 통합 테스트")
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CookieUtil cookieUtil;

    private User testUser;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = createTestUser(
                "test@example.com",
                "테스트유저",
                UserRole.USER,
                "123456789",
                null,
                null,
                null
        );

        // Mock HTTP 요청/응답 초기화
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("refreshToken() 테스트_성공")
    void testRefreshToken_Success() {
        // given
        String refreshToken = jwtTokenProvider.createRefreshToken(testUser);
        refreshTokenRepository.save(testUser.getEmail(), refreshToken);
        
        Cookie cookie = cookieUtil.createRefreshTokenCookie(refreshToken);
        request.setCookies(cookie);

        // when
        AccessTokenResponse result = authService.refreshToken(request, response);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isNotNull();
        assertThat(result.userResponse()).isNotNull();
        assertThat(result.userResponse().id()).isEqualTo(testUser.getId());
        assertThat(result.userResponse().email()).isEqualTo("test@example.com");
        
        // 새로운 Refresh Token이 Cookie에 설정되었는지 확인
        Cookie[] cookies = response.getCookies();
        assertThat(cookies).isNotEmpty();
        boolean hasRefreshToken = false;
        String newRefreshTokenFromCookie = null;
        for (Cookie c : cookies) {
            if ("refresh_token".equals(c.getName())) {
                hasRefreshToken = true;
                newRefreshTokenFromCookie = c.getValue();
                assertThat(newRefreshTokenFromCookie).isNotNull();
                break;
            }
        }
        assertThat(hasRefreshToken).isTrue();
        
        // 새로운 Refresh Token이 저장되었는지 확인
        String storedToken = refreshTokenRepository.findByEmail(testUser.getEmail());
        assertThat(storedToken).isNotNull();
        
        // Cookie의 새 토큰과 저장된 토큰이 일치하는지 확인
        if (newRefreshTokenFromCookie != null) {
            assertThat(storedToken).isEqualTo(newRefreshTokenFromCookie);
            // 새로운 토큰이 유효한지 확인
            assertThat(jwtTokenProvider.validateToken(newRefreshTokenFromCookie)).isTrue();
            assertThat(jwtTokenProvider.isRefreshToken(newRefreshTokenFromCookie)).isTrue();
        }
    }

    @Test
    @DisplayName("refreshToken() 테스트_실패_RefreshToken 없음")
    void testRefreshToken_Fail_RefreshTokenNotFound() {
        // given - Cookie 없음
        request.setCookies();

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request, response))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    @Test
    @DisplayName("refreshToken() 테스트_실패_유효하지 않은 RefreshToken")
    void testRefreshToken_Fail_InvalidRefreshToken() {
        // given
        String invalidToken = "invalid-token";
        Cookie cookie = new Cookie("refresh_token", invalidToken);
        request.setCookies(cookie);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request, response))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        
        // Cookie 삭제가 설정되었는지 확인
        Cookie[] cookies = response.getCookies();
        assertThat(cookies).isNotEmpty();
    }

    @Test
    @DisplayName("refreshToken() 테스트_실패_저장된 RefreshToken과 불일치")
    void testRefreshToken_Fail_StoredTokenMismatch() {
        // given
        String refreshToken = jwtTokenProvider.createRefreshToken(testUser);
        // 다른 토큰을 저장
        refreshTokenRepository.save(testUser.getEmail(), "different-token");
        
        Cookie cookie = cookieUtil.createRefreshTokenCookie(refreshToken);
        request.setCookies(cookie);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request, response))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("saveOrUpdateOAuth2User() 테스트_성공_새 사용자 저장")
    void testSaveOrUpdateOAuth2User_Success_NewUser() {
        // given
        User newUser = User.builder()
                .email("newuser@example.com")
                .name("새사용자")
                .role(null) // null로 설정하여 기본값 USER가 적용되는지 확인
                .provider(OAuth2Provider.GOOGLE)
                .providerId("google-999")
                .build();

        // when
        User result = authService.saveOrUpdateOAuth2User(newUser);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getEmail()).isEqualTo("newuser@example.com");
        assertThat(result.getName()).isEqualTo("새사용자");
        assertThat(result.getRole()).isEqualTo(UserRole.USER); // 기본값 적용 확인
        assertThat(result.getProvider()).isEqualTo(OAuth2Provider.GOOGLE);
        assertThat(result.getProviderId()).isEqualTo("google-999");
        
        // DB에서 실제로 저장되었는지 확인
        User savedUser = userRepository.findById(result.getId()).orElseThrow();
        assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
    }

    @Test
    @DisplayName("saveOrUpdateOAuth2User() 테스트_성공_기존 사용자 업데이트")
    void testSaveOrUpdateOAuth2User_Success_UpdateExistingUser() {
        // given
        User existingUser = createTestUser(
                "existing@example.com",
                "기존사용자",
                UserRole.USER,
                "google-888",
                null,
                null,
                null
        );
        
        User updateUser = User.builder()
                .email("updated@example.com")
                .name("업데이트된사용자")
                .role(UserRole.SELLER) // Role은 변경되지 않아야 함
                .provider(OAuth2Provider.GOOGLE)
                .providerId("google-888")
                .profileImageUrl("https://new-image.com/image.jpg")
                .build();

        // when
        User result = authService.saveOrUpdateOAuth2User(updateUser);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(existingUser.getId());
        assertThat(result.getEmail()).isEqualTo("updated@example.com");
        assertThat(result.getName()).isEqualTo("업데이트된사용자");
        assertThat(result.getRole()).isEqualTo(UserRole.USER); // 기존 Role 유지
        assertThat(result.getProviderId()).isEqualTo("google-888");
        
        // DB에서 실제로 업데이트되었는지 확인
        User updatedUser = userRepository.findById(existingUser.getId()).orElseThrow();
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getName()).isEqualTo("업데이트된사용자");
        assertThat(updatedUser.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("saveOrUpdateOAuth2User() 테스트_성공_프로필 이미지가 없을 때만 업데이트")
    void testSaveOrUpdateOAuth2User_Success_ProfileImageUpdate() {
        // given
        User existingUser = createTestUser(
                "image@example.com",
                "이미지사용자",
                UserRole.USER,
                "google-777",
                null,
                null,
                null
        );
        existingUser.setProfileImageUrl("https://old-image.com/image.jpg");
        existingUser = userRepository.save(existingUser);
        
        User updateUser = User.builder()
                .email("image@example.com")
                .name("이미지사용자")
                .provider(OAuth2Provider.GOOGLE)
                .providerId("google-777")
                .profileImageUrl("https://new-image.com/image.jpg")
                .build();

        // when
        User result = authService.saveOrUpdateOAuth2User(updateUser);

        // then
        // 기존 이미지가 있으면 유지되어야 함
        assertThat(result.getProfileImageUrl()).isEqualTo("https://old-image.com/image.jpg");
        
        // 기존 이미지가 없으면 새 이미지로 업데이트
        existingUser.setProfileImageUrl(null);
        existingUser = userRepository.save(existingUser);
        User result2 = authService.saveOrUpdateOAuth2User(updateUser);
        assertThat(result2.getProfileImageUrl()).isEqualTo("https://new-image.com/image.jpg");
    }

    @Test
    @DisplayName("completeSignup() 테스트_성공")
    void testCompleteSignup_Success() {
        // given
        String refreshToken = jwtTokenProvider.createRefreshToken(testUser);
        refreshTokenRepository.save(testUser.getEmail(), refreshToken);
        
        Cookie cookie = cookieUtil.createRefreshTokenCookie(refreshToken);
        request.setCookies(cookie);
        
        SignupRequest signupRequest = new SignupRequest(
                "010-1234-5678",
                LocalDate.of(1990, 1, 1),
                "서울시 강남구"
        );

        // when
        SignUpResponse result = authService.completeSignup(signupRequest, request, response);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isNotNull();
        assertThat(result.userResponse()).isNotNull();
        assertThat(result.userResponse().id()).isEqualTo(testUser.getId());
        assertThat(result.userResponse().phoneNumber()).isEqualTo("010-1234-5678");
        assertThat(result.userResponse().birthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(result.userResponse().address()).isEqualTo("서울시 강남구");
        
        // DB에서 실제로 업데이트되었는지 확인
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getPhoneNumber()).isEqualTo("010-1234-5678");
        assertThat(updatedUser.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(updatedUser.getAddress()).isEqualTo("서울시 강남구");
        
        // 새로운 Refresh Token이 Cookie에 설정되었는지 확인
        Cookie[] cookies = response.getCookies();
        assertThat(cookies).isNotEmpty();
    }

    @Test
    @DisplayName("completeSignup() 테스트_실패_생일이 미래 날짜")
    void testCompleteSignup_Fail_FutureBirthDate() {
        // given
        String refreshToken = jwtTokenProvider.createRefreshToken(testUser);
        refreshTokenRepository.save(testUser.getEmail(), refreshToken);
        
        Cookie cookie = cookieUtil.createRefreshTokenCookie(refreshToken);
        request.setCookies(cookie);
        
        SignupRequest signupRequest = new SignupRequest(
                "010-1234-5678",
                LocalDate.now().plusDays(1), // 미래 날짜
                "서울시 강남구"
        );

        // when & then
        assertThatThrownBy(() -> authService.completeSignup(signupRequest, request, response))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_BIRTH_DATE);
        
        // DB에서 업데이트되지 않았는지 확인
        User user = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(user.getPhoneNumber()).isNull();
        assertThat(user.getBirthDate()).isNull();
    }

    @Test
    @DisplayName("completeSignup() 테스트_실패_RefreshToken 없음")
    void testCompleteSignup_Fail_RefreshTokenNotFound() {
        // given
        request.setCookies();
        
        SignupRequest signupRequest = new SignupRequest(
                "010-1234-5678",
                LocalDate.of(1990, 1, 1),
                "서울시 강남구"
        );

        // when & then
        assertThatThrownBy(() -> authService.completeSignup(signupRequest, request, response))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    @Test
    @DisplayName("completeSignup() 테스트_실패_RefreshToken 유효하지 않음")
    void testCompleteSignup_Fail_InvalidRefreshToken() {
        // given
        String invalidToken = "invalid-token";
        Cookie cookie = new Cookie("refresh_token", invalidToken);
        request.setCookies(cookie);
        
        SignupRequest signupRequest = new SignupRequest(
                "010-1234-5678",
                LocalDate.of(1990, 1, 1),
                "서울시 강남구"
        );

        // when & then
        assertThatThrownBy(() -> authService.completeSignup(signupRequest, request, response))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }


    @Test
    @DisplayName("logout() 테스트_성공")
    void testLogout_Success() {
        // given
        String accessToken = jwtTokenProvider.createAccessToken(testUser);
        String refreshToken = jwtTokenProvider.createRefreshToken(testUser);
        refreshTokenRepository.save(testUser.getEmail(), refreshToken);
        
        request.addHeader("Authorization", "Bearer " + accessToken);
        Cookie cookie = cookieUtil.createRefreshTokenCookie(refreshToken);
        request.setCookies(cookie);

        // when
        authService.logout(request, response);

        // then
        // Refresh Token이 삭제되었는지 확인
        String storedToken = refreshTokenRepository.findByEmail(testUser.getEmail());
        assertThat(storedToken).isNull();
        
        // Cookie 삭제가 설정되었는지 확인
        Cookie[] cookies = response.getCookies();
        assertThat(cookies).isNotEmpty();
        boolean hasDeleteCookie = false;
        for (Cookie c : cookies) {
            if ("refresh_token".equals(c.getName()) && c.getMaxAge() == 0) {
                hasDeleteCookie = true;
                break;
            }
        }
        assertThat(hasDeleteCookie).isTrue();
    }

    @Test
    @DisplayName("logout() 테스트_성공_Authorization 헤더 없음")
    void testLogout_Success_NoAuthorizationHeader() {
        // given
        String refreshToken = jwtTokenProvider.createRefreshToken(testUser);
        refreshTokenRepository.save(testUser.getEmail(), refreshToken);
        
        Cookie cookie = cookieUtil.createRefreshTokenCookie(refreshToken);
        request.setCookies(cookie);
        // Authorization 헤더 없음

        // when
        authService.logout(request, response);

        // then
        // Refresh Token은 삭제되지 않음 (Authorization 헤더가 없으므로)
        String storedToken = refreshTokenRepository.findByEmail(testUser.getEmail());
        assertThat(storedToken).isNotNull();
        
        // Cookie 삭제는 설정됨
        Cookie[] cookies = response.getCookies();
        assertThat(cookies).isNotEmpty();
    }

    @Test
    @DisplayName("logout() 테스트_성공_유효하지 않은 AccessToken")
    void testLogout_Success_InvalidAccessToken() {
        // given
        String refreshToken = jwtTokenProvider.createRefreshToken(testUser);
        refreshTokenRepository.save(testUser.getEmail(), refreshToken);
        
        request.addHeader("Authorization", "Bearer invalid-token");
        Cookie cookie = cookieUtil.createRefreshTokenCookie(refreshToken);
        request.setCookies(cookie);

        // when
        authService.logout(request, response);

        // then
        // Refresh Token은 삭제되지 않음 (유효하지 않은 토큰이므로)
        String storedToken = refreshTokenRepository.findByEmail(testUser.getEmail());
        assertThat(storedToken).isNotNull();
        
        // Cookie 삭제는 설정됨
        Cookie[] cookies = response.getCookies();
        assertThat(cookies).isNotEmpty();
    }

    @Test
    @DisplayName("전체 플로우 테스트_OAuth2 사용자 생성부터 회원가입 완료까지")
    void testFullFlow_OAuth2SignupToComplete() {
        // 1. OAuth2 사용자 생성
        User oauthUser = User.builder()
                .email("oauth@example.com")
                .name("OAuth사용자")
                .provider(OAuth2Provider.GOOGLE)
                .providerId("google-555")
                .build();
        
        User savedOAuthUser = authService.saveOrUpdateOAuth2User(oauthUser);
        assertThat(savedOAuthUser.getId()).isNotNull();
        assertThat(savedOAuthUser.getRole()).isEqualTo(UserRole.USER);

        // 2. Refresh Token 생성 및 저장
        String refreshToken = jwtTokenProvider.createRefreshToken(savedOAuthUser);
        refreshTokenRepository.save(savedOAuthUser.getEmail(), refreshToken);
        
        Cookie cookie = cookieUtil.createRefreshTokenCookie(refreshToken);
        request.setCookies(cookie);

        // 3. 회원가입 완료 (getUserFromRefreshToken은 내부적으로 호출됨)
        SignupRequest signupRequest = new SignupRequest(
                "010-9999-8888",
                LocalDate.of(1995, 5, 15),
                "부산시 해운대구"
        );
        
        SignUpResponse signupResponse = authService.completeSignup(signupRequest, request, response);
        assertThat(signupResponse.userResponse().phoneNumber()).isEqualTo("010-9999-8888");
        assertThat(signupResponse.userResponse().address()).isEqualTo("부산시 해운대구");

        // 5. 새로운 Refresh Token으로 토큰 갱신
        Cookie[] cookies = response.getCookies();
        String newRefreshToken = null;
        for (Cookie c : cookies) {
            if ("refresh_token".equals(c.getName())) {
                newRefreshToken = c.getValue();
                break;
            }
        }
        assertThat(newRefreshToken).isNotNull();
        
        request.setCookies(cookieUtil.createRefreshTokenCookie(newRefreshToken));
        AccessTokenResponse tokenResponse = authService.refreshToken(request, response);
        assertThat(tokenResponse.accessToken()).isNotNull();

        // 6. 로그아웃
        request.addHeader("Authorization", "Bearer " + tokenResponse.accessToken());
        authService.logout(request, response);
        
        // Refresh Token이 삭제되었는지 확인
        String storedToken = refreshTokenRepository.findByEmail(savedOAuthUser.getEmail());
        assertThat(storedToken).isNull();
    }

    /**
     * 테스트용 사용자 생성 헬퍼 메서드
     */
    private User createTestUser(
            String email,
            String name,
            UserRole role,
            String providerId,
            String phoneNumber,
            String address,
            LocalDate birthDate
    ) {
        User user = User.builder()
                .email(email)
                .name(name)
                .role(role)
                .provider(OAuth2Provider.GOOGLE)
                .providerId(providerId)
                .phoneNumber(phoneNumber)
                .address(address)
                .birthDate(birthDate)
                .build();
        return userRepository.save(user);
    }
}

