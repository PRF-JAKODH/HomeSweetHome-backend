package com.homesweet.homesweetback.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.s3.ImageUploader;
import com.homesweet.homesweetback.domain.auth.dto.UpdateUserRequest;
import com.homesweet.homesweetback.domain.auth.dto.UpdateUserRoleRequest;
import com.homesweet.homesweetback.domain.auth.dto.UserResponse;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.grade.entity.Grade;
import com.homesweet.homesweetback.domain.grade.repository.GradeRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ImageUploader imageUploader;

    @Mock
    private GradeRepository gradeRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("getUserById() 메서드 테스트_성공")
    void testGetUserById_Success() {
        // given
        Long userId = 1L;
        User user = createTestUser(userId, UserRole.USER);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        User result = userService.getUserById(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("test@test.com");
        assertThat(result.getName()).isEqualTo("test");
        assertThat(result.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("getUserById() 메서드 테스트_실패_사용자 없음")
    void testGetUserById_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getUserInfo() 메서드 테스트_성공")
    void testGetUserInfo_Success() {
        // given
        Long userId = 1L;
        User user = createTestUser(userId, UserRole.USER);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        UserResponse response = userService.getUserInfo(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.name()).isEqualTo("test");
        assertThat(response.role()).isEqualTo(UserRole.USER);
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getUserInfo() 메서드 테스트_실패_사용자 없음")
    void testGetUserInfo_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserInfo(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("updateUserInfo() 메서드 테스트_성공_이미지 없음")
    void testUpdateUserInfo_Success_WithoutImage() {
        // given
        Long userId = 1L;
        User user = createTestUser(userId, UserRole.USER);
        UpdateUserRequest request = new UpdateUserRequest(
                "새이름",
                "010-1234-5678",
                LocalDate.of(1990, 1, 1),
                "서울시 강남구"
        );
        Optional<MultipartFile> profileImage = Optional.empty();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        UserResponse response = userService.updateUserInfo(userId, request, profileImage);

        // then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("새이름");
        assertThat(response.phoneNumber()).isEqualTo("010-1234-5678");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(response.address()).isEqualTo("서울시 강남구");
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(user);
        verify(imageUploader, never()).delete(anyString());
        verify(imageUploader, never()).upload(any(MultipartFile.class), anyString());
    }

    @Test
    @DisplayName("updateUserInfo() 메서드 테스트_성공_이미지 있음")
    void testUpdateUserInfo_Success_WithImage() {
        // given
        Long userId = 1L;
        User user = createTestUser(userId, UserRole.USER);
        user.setProfileImageUrl("https://old-image-url.com/image.jpg");
        UpdateUserRequest request = new UpdateUserRequest(
                "새이름",
                null,
                null,
                null
        );
        MultipartFile mockFile = org.mockito.Mockito.mock(MultipartFile.class);
        Optional<MultipartFile> profileImage = Optional.of(mockFile);
        String newImageUrl = "https://new-image-url.com/image.jpg";

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        willDoNothing().given(imageUploader).delete(anyString());
        given(imageUploader.upload(any(MultipartFile.class), anyString())).willReturn(newImageUrl);
        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        UserResponse response = userService.updateUserInfo(userId, request, profileImage);

        // then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("새이름");
        verify(userRepository, times(1)).findById(userId);
        verify(imageUploader, times(1)).delete("https://old-image-url.com/image.jpg");
        verify(imageUploader, times(1)).upload(mockFile, "user/profile/" + userId);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("updateUserInfo() 메서드 테스트_실패_사용자 없음")
    void testUpdateUserInfo_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        UpdateUserRequest request = new UpdateUserRequest(null, null, null, null);
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request, Optional.empty()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("updateUserInfo() 메서드 테스트_실패_핸드폰 번호 형식 오류")
    void testUpdateUserInfo_Fail_InvalidPhoneNumber() {
        // given
        Long userId = 1L;
        User user = createTestUser(userId, UserRole.USER);
        UpdateUserRequest request = new UpdateUserRequest(
                null,
                "123-456-789", // 잘못된 형식
                null,
                null
        );
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request, Optional.empty()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INVALID_PHONE_NUMBER_FORMAT.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateUserInfo() 메서드 테스트_실패_이미지 삭제 실패")
    void testUpdateUserInfo_Fail_ImageDeleteFailed() {
        // given
        Long userId = 1L;
        User user = createTestUser(userId, UserRole.USER);
        user.setProfileImageUrl("https://old-image-url.com/image.jpg");
        UpdateUserRequest request = new UpdateUserRequest(null, null, null, null);
        MultipartFile mockFile = org.mockito.Mockito.mock(MultipartFile.class);
        Optional<MultipartFile> profileImage = Optional.of(mockFile);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        willThrow(new RuntimeException("S3 삭제 실패")).given(imageUploader).delete(anyString());

        // when & then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request, profileImage))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.FILE_STREAM_ERROR.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(imageUploader, times(1)).delete(anyString());
        verify(imageUploader, never()).upload(any(MultipartFile.class), anyString());
    }

    @Test
    @DisplayName("updateUserInfo() 메서드 테스트_실패_이미지 업로드 실패")
    void testUpdateUserInfo_Fail_ImageUploadFailed() {
        // given
        Long userId = 1L;
        User user = createTestUser(userId, UserRole.USER);
        user.setProfileImageUrl("https://old-image-url.com/image.jpg");
        UpdateUserRequest request = new UpdateUserRequest(null, null, null, null);
        MultipartFile mockFile = org.mockito.Mockito.mock(MultipartFile.class);
        Optional<MultipartFile> profileImage = Optional.of(mockFile);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        willDoNothing().given(imageUploader).delete(anyString());
        willThrow(new RuntimeException("S3 업로드 실패")).given(imageUploader).upload(any(MultipartFile.class), anyString());

        // when & then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request, profileImage))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.FILE_STREAM_ERROR.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(imageUploader, times(1)).delete(anyString());
        verify(imageUploader, times(1)).upload(any(MultipartFile.class), anyString());
    }

    @Test
    @DisplayName("deleteUser() 메서드 테스트_성공")
    void testDeleteUser_Success() {
        // given
        Long userId = 1L;
        User user = createTestUser(userId, UserRole.USER);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        willDoNothing().given(userRepository).delete(user);

        // when
        userService.deleteUser(userId);

        // then
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    @DisplayName("deleteUser() 메서드 테스트_실패_사용자 없음")
    void testDeleteUser_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    @DisplayName("updateUserRole() 메서드 테스트_성공")
    void testUpdateUserRole_Success() {
        // given
        Long userId = 1L;
        User user = createTestUser(userId, UserRole.USER);
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRole.SELLER);
        Grade grade = createTestGrade(1, "GOLD", new BigDecimal("5.00"));

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        // ThreadLocalRandom으로 1~5 사이의 랜덤 값이 생성되므로 any()로 처리
        given(gradeRepository.findById(any(Integer.class))).willReturn(Optional.of(grade));
        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        UserResponse response = userService.updateUserRole(userId, request);

        // then
        assertThat(response).isNotNull();
        verify(userRepository, times(1)).findById(userId);
        verify(gradeRepository, times(1)).findById(any(Integer.class));
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("updateUserRole() 메서드 테스트_실패_사용자 없음")
    void testUpdateUserRole_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRole.SELLER);
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateUserRole(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(gradeRepository, never()).findById(any(Integer.class));
    }

    @Test
    @DisplayName("getUserGrade() 메서드 테스트_성공_등급 있음")
    void testGetUserGrade_Success_WithGrade() {
        // given
        Long userId = 1L;
        Grade grade = createTestGrade(1, "GOLD", new BigDecimal("5.00"));
        User user = createTestUser(userId, UserRole.USER);
        user.setGrade(grade);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        Optional<Grade> result = userService.getUserGrade(userId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getGrade()).isEqualTo("GOLD");
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getUserGrade() 메서드 테스트_성공_등급 없음")
    void testGetUserGrade_Success_WithoutGrade() {
        // given
        Long userId = 1L;
        User user = createTestUser(userId, UserRole.USER);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        Optional<Grade> result = userService.getUserGrade(userId);

        // then
        assertThat(result).isEmpty();
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getUserGrade() 메서드 테스트_실패_사용자 없음")
    void testGetUserGrade_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserGrade(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getUserGradeName() 메서드 테스트_성공_등급 있음")
    void testGetUserGradeName_Success_WithGrade() {
        // given
        Long userId = 1L;
        Grade grade = createTestGrade(1, "GOLD", new BigDecimal("5.00"));
        User user = createTestUser(userId, UserRole.USER);
        user.setGrade(grade);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        String gradeName = userService.getUserGradeName(userId);

        // then
        assertThat(gradeName).isEqualTo("GOLD");
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getUserGradeName() 메서드 테스트_성공_등급 없음")
    void testGetUserGradeName_Success_WithoutGrade() {
        // given
        Long userId = 1L;
        User user = createTestUser(userId, UserRole.USER);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        String gradeName = userService.getUserGradeName(userId);

        // then
        assertThat(gradeName).isEqualTo("등급 없음");
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getUserGradeName() 메서드 테스트_실패_사용자 없음")
    void testGetUserGradeName_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserGradeName(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getUserFeeRate() 메서드 테스트_성공_등급 있음")
    void testGetUserFeeRate_Success_WithGrade() {
        // given
        Long userId = 1L;
        BigDecimal feeRate = new BigDecimal("5.00");
        Grade grade = createTestGrade(1, "GOLD", feeRate);
        User user = createTestUser(userId, UserRole.USER);
        user.setGrade(grade);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        BigDecimal result = userService.getUserFeeRate(userId);

        // then
        assertThat(result).isEqualByComparingTo(feeRate);
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getUserFeeRate() 메서드 테스트_성공_등급 없음")
    void testGetUserFeeRate_Success_WithoutGrade() {
        // given
        Long userId = 1L;
        User user = createTestUser(userId, UserRole.USER);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        BigDecimal result = userService.getUserFeeRate(userId);

        // then
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getUserFeeRate() 메서드 테스트_실패_사용자 없음")
    void testGetUserFeeRate_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserFeeRate(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("hasUserGrade() 메서드 테스트_성공_등급 있음")
    void testHasUserGrade_Success_WithGrade() {
        // given
        Long userId = 1L;
        Grade grade = createTestGrade(1, "GOLD", new BigDecimal("5.00"));
        User user = createTestUser(userId, UserRole.USER);
        user.setGrade(grade);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        boolean result = userService.hasUserGrade(userId);

        // then
        assertThat(result).isTrue();
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("hasUserGrade() 메서드 테스트_성공_등급 없음")
    void testHasUserGrade_Success_WithoutGrade() {
        // given
        Long userId = 1L;
        User user = createTestUser(userId, UserRole.USER);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        boolean result = userService.hasUserGrade(userId);

        // then
        assertThat(result).isFalse();
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("hasUserGrade() 메서드 테스트_실패_사용자 없음")
    void testHasUserGrade_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.hasUserGrade(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
        verify(userRepository, times(1)).findById(userId);
    }

    private User createTestUser(Long userId, UserRole role) {
        User user = User.builder()
                .email("test@test.com")
                .name("test")
                .role(role)
                .provider(OAuth2Provider.GOOGLE)
                .providerId("123456789")
                .build();
        user.setId(userId);
        return user;
    }

    private Grade createTestGrade(Integer gradeId, String gradeName, BigDecimal feeRate) {
        // AllArgsConstructor를 사용하여 gradeId 포함
        return new Grade(gradeId, gradeName, feeRate);
    }
}

