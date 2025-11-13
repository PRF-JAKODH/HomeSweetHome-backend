package com.homesweet.homesweetback.domain.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.homesweet.homesweetback.common.config.QueryDslConfig;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;
import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;
import com.homesweet.homesweetback.domain.notification.entity.NotificationCategory;
import com.homesweet.homesweetback.domain.notification.entity.NotificationTemplate;
import com.homesweet.homesweetback.domain.notification.entity.UserNotification;
import com.homesweet.homesweetback.domain.product.category.repository.impl.ProductCategoryRepositoryImpl;
import com.homesweet.homesweetback.domain.product.category.repository.mapper.ProductCategoryMapper;

@ActiveProfiles("test")
@DataJpaTest // Transaction 포함 되어 있음
// ProductRepository의 의존성이 필요해서 추가
@Import({
    QueryDslConfig.class,
    ProductCategoryRepositoryImpl.class,
    ProductCategoryMapper.class
})
public class UserNotificationRepositoryTest {
    
    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private UserRepository userRepository;  

    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;

    @Autowired
    private NotificationCategoryRepository notificationCategoryRepository;

    // BeforeEach에서 설정한 데이터를 테스트에서 재사용
    private User testUser;
    private NotificationCategory testCategory;
    private NotificationTemplate testTemplate;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(createTestUser());

        testCategory = NotificationCategory.builder()
                .categoryType(NotificationCategoryType.ORDER)
                .build();
        notificationCategoryRepository.save(testCategory);

        testTemplate = createTestNotificationTemplate(testCategory);
        notificationTemplateRepository.save(testTemplate);
    }

    @Test
    @DisplayName("사용자 알림 저장 테스트_성공")
    void saveUserNotification_Success() {
        // Given
        // setUp()에서 설정된 testUser, testTemplate 사용

        // When
        UserNotification userNotification = UserNotification.builder()
                .user(testUser)
                .template(testTemplate)
                .contextData(Map.of("orderId", "123456789"))
                .isRead(false)
                .isDeleted(false)
                .build();
        UserNotification savedUserNotification = userNotificationRepository.save(userNotification);

        // Then
        assertThat(savedUserNotification.getId()).isNotNull();
        assertThat(savedUserNotification.getUser()).isEqualTo(testUser);
        assertThat(savedUserNotification.getTemplate()).isEqualTo(testTemplate);
        assertThat(savedUserNotification.getContextData()).isEqualTo(Map.of("orderId", "123456789"));
        assertThat(savedUserNotification.getIsRead()).isEqualTo(false);
        assertThat(savedUserNotification.getIsDeleted()).isEqualTo(false);
    }

    @Test
    @DisplayName("사용자 알림 조회 테스트_성공")
    void findUserNotification_Success() {
        // Given
        // setUp()에서 설정된 testUser, testTemplate 사용
        UserNotification userNotification = createTestUserNotification(testUser, testTemplate);
        UserNotification savedUserNotification = userNotificationRepository.save(userNotification);

        // When
        UserNotification foundUserNotification = userNotificationRepository.findById(savedUserNotification.getId()).orElseThrow();

        // Then
        assertThat(foundUserNotification.getId()).isNotNull();
        assertThat(foundUserNotification.getUser()).isEqualTo(testUser);
        assertThat(foundUserNotification.getTemplate()).isEqualTo(testTemplate);
        assertThat(foundUserNotification.getContextData()).isEqualTo(Map.of("orderId", savedUserNotification.getContextData().get("orderId")));
        assertThat(foundUserNotification.getIsRead()).isEqualTo(false);
        assertThat(foundUserNotification.getIsDeleted()).isEqualTo(false);
    }


    @Test
    @DisplayName("특정 사용자의 알림 목록 최신 20개 조회_성공")
    void findTop20UserNotificationList_Success() {
        // Given
        // setUp()에서 설정된 testUser, testTemplate 사용

        List<UserNotification> savedUserNotificationList = createTestUserNotificationList(testUser, testTemplate, 25);
        savedUserNotificationList.forEach(userNotificationRepository::save);

        // When
        List<UserNotification> userNotificationList = userNotificationRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(testUser.getId());

        // Then
        assertThat(userNotificationList.size()).isEqualTo(20);
    }

    @Test
    @DisplayName("알림이 20개 이하일 때 모두 조회_성공")
    void findTop20UserNotificationList_Success_LessThan20() {
        // Given
        // setUp()에서 설정된 testUser, testTemplate 사용

        List<UserNotification> savedUserNotificationList = createTestUserNotificationList(testUser, testTemplate, 15);
        savedUserNotificationList.forEach(userNotificationRepository::save);

        // When
        List<UserNotification> userNotificationList = userNotificationRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(testUser.getId());

        // Then
        assertThat(userNotificationList.size()).isEqualTo(15);
    }

    @Test
    @DisplayName("알림이 없을 때 빈 리스트 반환_성공")
    void findTop20UserNotificationList_Success_Empty() {
        // Given
        // setUp()에서 설정된 testUser 사용

        // When
        List<UserNotification> userNotificationList = userNotificationRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(testUser.getId());

        // Then
        assertThat(userNotificationList.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("삭제되지 않은 알림만 조회_성공")
    void findTop20UserNotificationList_Success_NotDeleted() {
        // Given
        UserNotification userNotification = createTestUserNotification(testUser, testTemplate);
        userNotification.markAsDeleted();
        userNotification = userNotificationRepository.save(userNotification);

        // When
        List<UserNotification> userNotificationList = userNotificationRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(testUser.getId());

        // Then
        assertThat(userNotificationList).isEmpty();
    }

    @Test
    @DisplayName("여러개의 알림 중 삭제되지 않은 알림 조회_성공")
    void findTop20UserNotificationList_Success_NotDeleted_Multiple() {
        // Given
        // setUp()에서 설정된 testUser, testTemplate 사용

        List<UserNotification> savedUserNotificationList = createTestUserNotificationList(testUser, testTemplate, 30);
        
        for (int i = 0; i < savedUserNotificationList.size(); i++) {
            UserNotification userNotification = savedUserNotificationList.get(i);
            if (i % 3 == 0) {
                userNotification.markAsDeleted();
            }
            userNotificationRepository.save(userNotification);
        }

        // When
        List<UserNotification> userNotificationList = userNotificationRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(testUser.getId());

        // Then
        assertThat(userNotificationList.size()).isEqualTo(20);
    }

    @Test
    @DisplayName("모든 알림이 삭제되어 있을 때 빈 리스트 반환_성공")
    void findTop20UserNotificationList_Success_AllDeleted() {
        // Given
        // setUp()에서 설정된 testUser, testTemplate 사용

        List<UserNotification> savedUserNotificationList = createTestUserNotificationList(testUser, testTemplate, 30);

        savedUserNotificationList.forEach(userNotification -> {
            userNotification.markAsDeleted();
            userNotificationRepository.save(userNotification);
        });

        // When
        List<UserNotification> userNotificationList = userNotificationRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(testUser.getId());

        // Then
        assertThat(userNotificationList).isEmpty();
    }

    @Test
    @DisplayName("사용자의 읽지 않은 알림 개수 조회_성공")
    void countUnreadUserNotification_Success() {
        // Given
        // setUp()에서 설정된 testUser, testTemplate 사용

        List<UserNotification> savedUserNotificationList = createTestUserNotificationList(testUser, testTemplate, 10);
        savedUserNotificationList.forEach(userNotificationRepository::save);

        // When
        long count = userNotificationRepository.countByUserIdAndIsReadFalseAndIsDeletedFalse(testUser.getId());

        // Then
        assertThat(count).isEqualTo(10);
    }

    @Test
    @DisplayName("읽지 않은 알림이 없을 때 0 반환_성공")
    void countUnreadUserNotification_Success_Empty() {
        // Given
        // setUp()에서 설정된 testUser, testTemplate 사용

        List<UserNotification> savedUserNotificationList = createTestUserNotificationList(testUser, testTemplate, 10);
        savedUserNotificationList.forEach(userNotification -> {
            userNotification.markAsRead();
            userNotificationRepository.save(userNotification);
        });

        // When
        long count = userNotificationRepository.countByUserIdAndIsReadFalseAndIsDeletedFalse(testUser.getId());

        // Then
        assertThat(count).isEqualTo(0);
    }


    @Test
    @DisplayName("사용자의 모든 읽지 않은 알림이 없을 때 빈 리스트 반환_성공")
    void findAllUnreadUserNotification_Success_Empty() {
        // Given
        // setUp()에서 설정된 testUser, testTemplate 사용

        List<UserNotification> savedUserNotificationList = createTestUserNotificationList(testUser, testTemplate, 10);
        savedUserNotificationList.forEach(userNotification -> {
            userNotification.markAsRead();
            userNotificationRepository.save(userNotification);
        });

        // When
        List<UserNotification> userNotificationList = userNotificationRepository.findByUserIdAndIsReadFalseAndIsDeletedFalse(testUser.getId());

        // Then
        assertThat(userNotificationList).isEmpty();
    }

    @Test
    @DisplayName("사용자의 모든 읽지 않은 알림 조회_성공")
    void findAllUnreadUserNotification_Success() {
        // Given
        // setUp()에서 설정된 testUser, testTemplate 사용
        
        List<UserNotification> savedUserNotificationList = createTestUserNotificationList(testUser, testTemplate, 10);
        for (int i = 0; i < savedUserNotificationList.size(); i++) {
            UserNotification userNotification = savedUserNotificationList.get(i);
            if (i % 2 == 0) {
                userNotification.markAsRead();
            }
            userNotificationRepository.save(userNotification);
        }

        // When
        List<UserNotification> userNotificationList = userNotificationRepository.findByUserIdAndIsReadFalseAndIsDeletedFalse(testUser.getId());

        // Then
        assertThat(userNotificationList.size()).isEqualTo(5);
        assertThat(userNotificationList.stream().allMatch(userNotification -> userNotification.getIsRead())).isFalse();
    }

    @Test
    @DisplayName("사용자의 삭제되지 않은 알림 조회_성공")
    void findByIdInAndUserIdAndNotDeleted_Success() {
        // Given
        // setUp()에서 설정된 testUser, testTemplate 사용
        
        List<UserNotification> savedUserNotificationList = createTestUserNotificationList(testUser, testTemplate, 30);
        for (int i = 0; i < savedUserNotificationList.size(); i++) {
            UserNotification userNotification = savedUserNotificationList.get(i);
            if (i % 2 == 0) {
                userNotification.markAsDeleted();
            }
            userNotificationRepository.save(userNotification);
        }

        List<Long> notificationIdsToDelete = savedUserNotificationList.stream().filter(userNotification -> !userNotification.getIsDeleted()).map(UserNotification::getId).toList();

        // When
        List<UserNotification> userNotificationList = userNotificationRepository.findByIdInAndUserIdAndNotDeleted(notificationIdsToDelete, testUser.getId());

        // Then
        assertThat(userNotificationList.size()).isEqualTo(15);
        assertThat(userNotificationList.stream().allMatch(userNotification -> userNotification.getIsDeleted())).isFalse();
    }

    private User createTestUser() {
        return User.builder()
                .email("test@example.com")
                .name("테스트 사용자")
                .address("서울시 강남구 역삼동")
                .phoneNumber("01012345678")
                .birthDate(LocalDate.of(1990, 1, 1))
                .profileImageUrl("https://example.com/profile.jpg")
                .provider(OAuth2Provider.KAKAO)
                .providerId("123456789")
                .role(UserRole.USER)
                .build();
    }

    private NotificationTemplate createTestNotificationTemplate(NotificationCategory category) {
        return NotificationTemplate.builder()
                .category(category)
                .templateType(NotificationTemplateType.ORDER_COMPLETED)
                .title("주문 완료")
                .content("주문이 완료되었습니다.")
                .redirectUrl("/order/{orderId}")
                .build();
    }

    private UserNotification createTestUserNotification(User user, NotificationTemplate template) {
        Long orderId = (long) (Math.random() * 1000000000);
        return UserNotification.builder()
                .user(user)
                .template(template)
                .contextData(Map.of("orderId", orderId))
                .isRead(false)
                .isDeleted(false)
                .build();   
    }

    private List<UserNotification> createTestUserNotificationList(User user, NotificationTemplate template, int count) {
        List<UserNotification> userNotificationList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UserNotification userNotification = createTestUserNotification(user, template);
            userNotificationList.add(userNotification);
        }
        return userNotificationList;
    }
}
