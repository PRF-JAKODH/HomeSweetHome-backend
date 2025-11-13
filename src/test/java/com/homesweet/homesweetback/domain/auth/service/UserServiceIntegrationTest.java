package com.homesweet.homesweetback.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

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

/**
 * UserService 테스트
 * - 실제 DB(H2)를 사용하여 전체 플로우 검증
 * - 트랜잭션 롤백으로 테스트 격리 보장
 * - ImageUploader는 MockitoBean으로 Mock 처리하여 S3 업로드 시뮬레이션
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UserService 테스트")
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @MockitoBean
    private ImageUploader imageUploader;

    private User testUser;
    private Grade testGrade;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = createTestUser(
                "test@example.com",
                "테스트유저",
                UserRole.USER,
                "123456789",
                "010-1234-5678",
                "서울시 강남구",
                LocalDate.of(1990, 1, 1)
        );

        testGrade = gradeRepository.findById(1).orElseThrow();

        // ImageUploader Mock 설정
        when(imageUploader.upload(any(MultipartFile.class), anyString()))
                .thenAnswer(invocation -> {
                    String path = invocation.getArgument(1);
                    return "https://test-bucket.s3.amazonaws.com/" + path + "/test-image.jpg";
                });
    }

    @Test
    @DisplayName("getUserInfo() 테스트_성공")
    void testGetUserInfo_Success() {
        // when
        UserResponse response = userService.getUserInfo(testUser.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(testUser.getId());
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.name()).isEqualTo("테스트유저");
        assertThat(response.role()).isEqualTo(UserRole.USER);
        assertThat(response.phoneNumber()).isEqualTo("010-1234-5678");
        assertThat(response.address()).isEqualTo("서울시 강남구");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
    }

    @Test
    @DisplayName("getUserInfo() 테스트_실패_사용자 없음")
    void testGetUserInfo_Fail_UserNotFound() {
        // given
        Long invalidUserId = 99999L;

        // when & then
        assertThatThrownBy(() -> userService.getUserInfo(invalidUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: " + invalidUserId);
    }

    @Test
    @DisplayName("updateUserInfo() 테스트_성공_이미지 없음")
    void testUpdateUserInfo_Success_WithoutImage() {
        // given
        UpdateUserRequest request = new UpdateUserRequest(
                "수정된이름",
                "010-9876-5432",
                LocalDate.of(1995, 5, 15),
                "서울시 서초구"
        );

        // when
        UserResponse response = userService.updateUserInfo(
                testUser.getId(),
                request,
                Optional.empty()
        );

        // then
        assertThat(response.name()).isEqualTo("수정된이름");
        assertThat(response.phoneNumber()).isEqualTo("010-9876-5432");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1995, 5, 15));
        assertThat(response.address()).isEqualTo("서울시 서초구");

        // DB에서 실제로 업데이트되었는지 확인
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getName()).isEqualTo("수정된이름");
        assertThat(updatedUser.getPhoneNumber()).isEqualTo("010-9876-5432");
        assertThat(updatedUser.getBirthDate()).isEqualTo(LocalDate.of(1995, 5, 15));
        assertThat(updatedUser.getAddress()).isEqualTo("서울시 서초구");
    }

    @Test
    @DisplayName("updateUserInfo() 테스트_성공_이미지 있음")
    void testUpdateUserInfo_Success_WithImage() {
        // given
        testUser.setProfileImageUrl("https://old-image-url.com/image.jpg");
        userRepository.save(testUser);

        MockMultipartFile image = new MockMultipartFile(
                "profileImage",
                "new-profile.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        UpdateUserRequest request = new UpdateUserRequest(
                "이미지업데이트유저",
                null,
                null,
                null
        );

        // when
        UserResponse response = userService.updateUserInfo(
                testUser.getId(),
                request,
                Optional.of(image)
        );

        // then
        assertThat(response.name()).isEqualTo("이미지업데이트유저");
        assertThat(response.profileImageUrl()).isNotNull();
        assertThat(response.profileImageUrl()).contains("test-bucket.s3.amazonaws.com");

        // DB에서 실제로 업데이트되었는지 확인
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getName()).isEqualTo("이미지업데이트유저");
        assertThat(updatedUser.getProfileImageUrl()).isNotNull();
    }

    @Test
    @DisplayName("updateUserInfo() 테스트_성공_부분 업데이트")
    void testUpdateUserInfo_Success_PartialUpdate() {
        // given
        UpdateUserRequest request = new UpdateUserRequest(
                "이름만수정",
                null,
                null,
                null
        );

        // when
        UserResponse response = userService.updateUserInfo(
                testUser.getId(),
                request,
                Optional.empty()
        );

        // then
        assertThat(response.name()).isEqualTo("이름만수정");
        // 기존 값들은 유지되어야 함
        assertThat(response.phoneNumber()).isEqualTo("010-1234-5678");
        assertThat(response.address()).isEqualTo("서울시 강남구");

        // DB 확인
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getName()).isEqualTo("이름만수정");
        assertThat(updatedUser.getPhoneNumber()).isEqualTo("010-1234-5678");
    }

    @Test
    @DisplayName("updateUserInfo() 테스트_실패_사용자 없음")
    void testUpdateUserInfo_Fail_UserNotFound() {
        // given
        Long invalidUserId = 99999L;
        UpdateUserRequest request = new UpdateUserRequest(null, null, null, null);

        // when & then
        assertThatThrownBy(() -> userService.updateUserInfo(
                invalidUserId,
                request,
                Optional.empty()
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: " + invalidUserId);
    }

    @Test
    @DisplayName("updateUserInfo() 테스트_실패_핸드폰 번호 형식 오류")
    void testUpdateUserInfo_Fail_InvalidPhoneNumber() {
        // given
        UpdateUserRequest request = new UpdateUserRequest(
                null,
                "123-456-789", // 잘못된 형식
                null,
                null
        );

        // when & then
        assertThatThrownBy(() -> userService.updateUserInfo(
                testUser.getId(),
                request,
                Optional.empty()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("올바른 핸드폰 번호 형식이 아닙니다");
    }

    @Test
    @DisplayName("deleteUser() 테스트_성공")
    void testDeleteUser_Success() {
        // given
        Long userId = testUser.getId();

        // when
        userService.deleteUser(userId);

        // then
        Optional<User> deletedUser = userRepository.findById(userId);
        assertThat(deletedUser).isEmpty();
    }

    @Test
    @DisplayName("deleteUser() 테스트_실패_사용자 없음")
    void testDeleteUser_Fail_UserNotFound() {
        // given
        Long invalidUserId = 99999L;

        // when & then
        assertThatThrownBy(() -> userService.deleteUser(invalidUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: " + invalidUserId);
    }

    @Test
    @DisplayName("updateUserRole() 테스트_성공")
    void testUpdateUserRole_Success() {
        // given
        // setUp에서 이미 5개의 등급이 생성되어 있음
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRole.SELLER);

        // when
        UserResponse response = userService.updateUserRole(testUser.getId(), request);

        // then
        assertThat(response.role()).isEqualTo(UserRole.SELLER);
        assertThat(response.grade()).isNotNull();

        // DB에서 실제로 업데이트되었는지 확인
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getRole()).isEqualTo(UserRole.SELLER);
        assertThat(updatedUser.getGrade()).isNotNull();
    }

    @Test
    @DisplayName("updateUserRole() 테스트_실패_사용자 없음")
    void testUpdateUserRole_Fail_UserNotFound() {
        // given
        Long invalidUserId = 99999L;
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRole.SELLER);

        // when & then
        assertThatThrownBy(() -> userService.updateUserRole(invalidUserId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: " + invalidUserId);
    }

    @Test
    @DisplayName("getUserGrade() 테스트_성공_등급 있음")
    void testGetUserGrade_Success_WithGrade() {
        // given
        testUser.setGrade(testGrade);
        userRepository.save(testUser);

        // when
        Optional<Grade> result = userService.getUserGrade(testUser.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getGrade()).isEqualTo(testGrade.getGrade());
        assertThat(result.get().getFeeRate()).isEqualByComparingTo(testGrade.getFeeRate());
    }

    @Test
    @DisplayName("getUserGrade() 테스트_성공_등급 없음")
    void testGetUserGrade_Success_WithoutGrade() {
        // when
        Optional<Grade> result = userService.getUserGrade(testUser.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getUserGrade() 테스트_실패_사용자 없음")
    void testGetUserGrade_Fail_UserNotFound() {
        // given
        Long invalidUserId = 99999L;

        // when & then
        assertThatThrownBy(() -> userService.getUserGrade(invalidUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: " + invalidUserId);
    }

    @Test
    @DisplayName("getUserGradeName() 테스트_성공_등급 있음")
    void testGetUserGradeName_Success_WithGrade() {
        // given
        testUser.setGrade(testGrade);
        userRepository.save(testUser);

        // when
        String gradeName = userService.getUserGradeName(testUser.getId());

        // then
        assertThat(gradeName).isEqualTo(testGrade.getGrade());
    }

    @Test
    @DisplayName("getUserGradeName() 테스트_성공_등급 없음")
    void testGetUserGradeName_Success_WithoutGrade() {
        // when
        String gradeName = userService.getUserGradeName(testUser.getId());

        // then
        assertThat(gradeName).isEqualTo("등급 없음");
    }

    @Test
    @DisplayName("getUserGradeName() 테스트_실패_사용자 없음")
    void testGetUserGradeName_Fail_UserNotFound() {
        // given
        Long invalidUserId = 99999L;

        // when & then
        assertThatThrownBy(() -> userService.getUserGradeName(invalidUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: " + invalidUserId);
    }

    @Test
    @DisplayName("getUserFeeRate() 테스트_성공_등급 있음")
    void testGetUserFeeRate_Success_WithGrade() {
        // given
        testUser.setGrade(testGrade);
        userRepository.save(testUser);

        // when
        BigDecimal feeRate = userService.getUserFeeRate(testUser.getId());

        // then
        assertThat(feeRate).isEqualByComparingTo(testGrade.getFeeRate());
    }

    @Test
    @DisplayName("getUserFeeRate() 테스트_성공_등급 없음")
    void testGetUserFeeRate_Success_WithoutGrade() {
        // when
        BigDecimal feeRate = userService.getUserFeeRate(testUser.getId());

        // then
        assertThat(feeRate).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getUserFeeRate() 테스트_실패_사용자 없음")
    void testGetUserFeeRate_Fail_UserNotFound() {
        // given
        Long invalidUserId = 99999L;

        // when & then
        assertThatThrownBy(() -> userService.getUserFeeRate(invalidUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: " + invalidUserId);
    }

    @Test
    @DisplayName("hasUserGrade() 테스트_성공_등급 있음")
    void testHasUserGrade_Success_WithGrade() {
        // given
        testUser.setGrade(testGrade);
        userRepository.save(testUser);

        // when
        boolean result = userService.hasUserGrade(testUser.getId());

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasUserGrade() 테스트_성공_등급 없음")
    void testHasUserGrade_Success_WithoutGrade() {
        // when
        boolean result = userService.hasUserGrade(testUser.getId());

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("hasUserGrade() 테스트_실패_사용자 없음")
    void testHasUserGrade_Fail_UserNotFound() {
        // given
        Long invalidUserId = 99999L;

        // when & then
        assertThatThrownBy(() -> userService.hasUserGrade(invalidUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: " + invalidUserId);
    }

    @Test
    @DisplayName("전체 플로우 테스트_사용자 생성부터 삭제까지")
    void testFullFlow_CreateToDelete() {
        // 1. 사용자 생성
        User newUser = createTestUser(
                "newuser@example.com",
                "새사용자",
                UserRole.USER,
                "987654321",
                null,
                null,
                null
        );

        // 2. 사용자 정보 조회
        UserResponse userInfo = userService.getUserInfo(newUser.getId());
        assertThat(userInfo.email()).isEqualTo("newuser@example.com");

        // 3. 사용자 정보 수정
        UpdateUserRequest updateRequest = new UpdateUserRequest(
                "수정된사용자",
                "010-1111-2222",
                LocalDate.of(2000, 1, 1),
                "부산시 해운대구"
        );
        UserResponse updatedInfo = userService.updateUserInfo(
                newUser.getId(),
                updateRequest,
                Optional.empty()
        );
        assertThat(updatedInfo.name()).isEqualTo("수정된사용자");

        // 4. 등급 설정
        newUser.setGrade(testGrade);
        newUser = userRepository.save(newUser);

        // 5. 등급 정보 조회
        Optional<Grade> grade = userService.getUserGrade(newUser.getId());
        assertThat(grade).isPresent();
        assertThat(userService.getUserGradeName(newUser.getId())).isEqualTo(testGrade.getGrade());
        assertThat(userService.hasUserGrade(newUser.getId())).isTrue();

        // 6. 역할 변경
        UpdateUserRoleRequest roleRequest = new UpdateUserRoleRequest(UserRole.SELLER);
        UserResponse roleUpdated = userService.updateUserRole(newUser.getId(), roleRequest);
        assertThat(roleUpdated.role()).isEqualTo(UserRole.SELLER);

        // 7. 사용자 삭제
        userService.deleteUser(newUser.getId());
        Optional<User> deletedUser = userRepository.findById(newUser.getId());
        assertThat(deletedUser).isEmpty();
    }

    /**
     * 테스트용 사용자 생성 헬퍼 메서드
     * 
     * @param email 이메일
     * @param name 이름
     * @param role 역할
     * @param providerId Provider ID
     * @param phoneNumber 핸드폰 번호 (선택)
     * @param address 주소 (선택)
     * @param birthDate 생년월일 (선택)
     * @return 생성된 User 엔티티
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

