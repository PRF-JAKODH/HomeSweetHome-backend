package com.homesweet.homesweetback.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;
import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;
import com.homesweet.homesweetback.domain.notification.entity.NotificationCategory;
import com.homesweet.homesweetback.domain.notification.entity.NotificationTemplate;
import com.homesweet.homesweetback.domain.notification.entity.UserNotification;
import com.homesweet.homesweetback.domain.notification.repository.NotificationCategoryRepository;
import com.homesweet.homesweetback.domain.notification.repository.NotificationTemplateRepository;
import com.homesweet.homesweetback.domain.notification.service.impl.UserNotificationService;

/**
 * UserNotificationService 엣지 케이스 테스트
 * 
 * Null 파라미터, 빈 컬렉션, 경계값, 대용량 데이터 등 다양한 엣지 케이스를 테스트합니다.
 * 
 * @author dogyungkim
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UserNotificationService 엣지 케이스 테스트")
public class UserNotificationServiceEdgeCaseTest {

    @Autowired
    private UserNotificationService userNotificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;

    @Autowired
    private NotificationCategoryRepository notificationCategoryRepository;

    private User testUser;
    private NotificationCategory testCategory;
    private NotificationTemplate testTemplate;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(createTestUser());

        testCategory = NotificationCategory.builder()
                .categoryType(NotificationCategoryType.ORDER)
                .build();

        testCategory = notificationCategoryRepository.save(testCategory);

        testTemplate = createTestNotificationTemplate(testCategory);
        notificationTemplateRepository.save(testTemplate);
    }

    // ========== Null 파라미터 테스트 ==========

    @Test
    @DisplayName("사용자 알림 생성 테스트_실패_Null userId")
    void createUserNotification_Failure_NullUserId() {
        // Given
        Map<String, Object> contextData = Map.of("orderId", 12345L);

        // When & Then
        assertThatThrownBy(() -> {
            userNotificationService.createUserNotification(
                    null,
                    testTemplate,
                    contextData);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자 ID는 null일 수 없습니다");
    }

    @Test
    @DisplayName("사용자 알림 생성 테스트_실패_Null template")
    void createUserNotification_Failure_NullTemplate() {
        // Given
        Map<String, Object> contextData = Map.of("orderId", 12345L);

        // When & Then
        assertThatThrownBy(() -> {
            userNotificationService.createUserNotification(
                    testUser,
                    null,
                    contextData);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("알림 템플릿은 null일 수 없습니다");
    }

    @Test
    @DisplayName("사용자 알림 생성 테스트_성공_Null contextData")
    void createUserNotification_Success_NullContextData() {
        // Given & When
        UserNotification notification = userNotificationService.createUserNotification(
                testUser,
                testTemplate,
                null);

        // Then
        assertThat(notification).isNotNull();
        assertThat(notification.getContextData()).isNotNull();
        assertThat(notification.getContextData()).isEmpty();
    }

    @Test
    @DisplayName("커스텀 알림 템플릿 생성 테스트_실패_Null title")
    void createAndSaveCustomNotificationTemplate_Failure_NullTitle() {
        // When & Then
        assertThatThrownBy(() -> {
            userNotificationService.createAndSaveCustomNotificationTemplate(
                    null,
                    "내용",
                    "app://test");
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("알림 제목은 비어있을 수 없습니다");
    }

    @Test
    @DisplayName("커스텀 알림 템플릿 생성 테스트_실패_Null content")
    void createAndSaveCustomNotificationTemplate_Failure_NullContent() {
        // When & Then
        assertThatThrownBy(() -> {
            userNotificationService.createAndSaveCustomNotificationTemplate(
                    "제목",
                    null,
                    "app://test");
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("알림 내용은 비어있을 수 없습니다");
    }

    @Test
    @DisplayName("커스텀 알림 템플릿 생성 테스트_성공_Null redirectUrl")
    void createAndSaveCustomNotificationTemplate_Success_NullRedirectUrl() {
        // Given
        String title = "제목";
        String content = "내용";

        // When
        NotificationTemplate savedTemplate = userNotificationService.createAndSaveCustomNotificationTemplate(
                title,
                content,
                null);

        // Then
        assertThat(savedTemplate.getId()).isNotNull();
        assertThat(savedTemplate.getTitle()).isEqualTo(title);
        assertThat(savedTemplate.getContent()).isEqualTo(content);
        assertThat(savedTemplate.getRedirectUrl()).isNotNull();
    }

    // ========== 빈 컬렉션 테스트 ==========

    @Test
    @DisplayName("사용자 알림 생성 테스트_성공_빈 contextData")
    void createUserNotification_Success_EmptyContextData() {
        // Given & When
        UserNotification notification = userNotificationService.createUserNotification(
                testUser,
                testTemplate,
                Map.of());

        // Then
        assertThat(notification).isNotNull();
        assertThat(notification.getContextData()).isNotNull();
        assertThat(notification.getContextData()).isEmpty();
    }

    @Test
    @DisplayName("대량 알림 저장 테스트_실패_Null 리스트")
    void bulkInsertUserNotifications_Failure_NullList() {
        // When & Then
        assertThatThrownBy(() -> {
            userNotificationService.bulkInsertUserNotifications(null);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자 알림 리스트는 null일 수 없습니다");
    }

    @Test
    @DisplayName("대량 알림 저장 테스트_성공_빈 리스트")
    void bulkInsertUserNotifications_Success_EmptyList() {
        // Given
        List<UserNotification> emptyList = new ArrayList<>();

        // When & Then - 예외가 발생하지 않아야 함
        assertThatCode(() -> {
            userNotificationService.bulkInsertUserNotifications(emptyList);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("커스텀 알림 템플릿 생성 테스트_실패_빈 title")
    void createAndSaveCustomNotificationTemplate_Failure_EmptyTitle() {
        // When & Then
        assertThatThrownBy(() -> {
            userNotificationService.createAndSaveCustomNotificationTemplate(
                    "   ",
                    "내용",
                    "app://test");
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("알림 제목은 비어있을 수 없습니다");
    }

    @Test
    @DisplayName("커스텀 알림 템플릿 생성 테스트_실패_빈 content")
    void createAndSaveCustomNotificationTemplate_Failure_EmptyContent() {
        // When & Then
        assertThatThrownBy(() -> {
            userNotificationService.createAndSaveCustomNotificationTemplate(
                    "제목",
                    "   ",
                    "app://test");
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("알림 내용은 비어있을 수 없습니다");
    }

    // ========== 대용량 데이터 테스트 ==========

    @Test
    @DisplayName("대량 알림 저장 테스트_성공_대용량 데이터_500개")
    void bulkInsertUserNotifications_Success_LargeDataset_500() {
        // Given
        List<UserNotification> notifications = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            UserNotification notification = userNotificationService.createUserNotification(
                    testUser,
                    testTemplate,
                    Map.of("orderId", i));
            notifications.add(notification);
        }

        // When & Then - 예외가 발생하지 않아야 함
        assertThatCode(() -> {
            userNotificationService.bulkInsertUserNotifications(notifications);
        }).doesNotThrowAnyException();
    }

    // ========== 복잡한 데이터 테스트 ==========

    @Test
    @DisplayName("사용자 알림 생성 테스트_성공_복잡한 contextData")
    void createUserNotification_Success_ComplexContextData() {
        // Given
        Map<String, Object> complexContextData = Map.of(
                "orderId", 12345L,
                "userName", "홍길동",
                "amount", 50000,
                "status", "completed",
                "items", List.of("item1", "item2", "item3"));

        // When
        UserNotification notification = userNotificationService.createUserNotification(
                testUser,
                testTemplate,
                complexContextData);

        // Then
        assertThat(notification).isNotNull();
        assertThat(notification.getContextData()).isEqualTo(complexContextData);
        assertThat(notification.getContextData().get("items")).isInstanceOf(List.class);
        assertThat(notification.getContextData().get("orderId")).isEqualTo(12345L);
        assertThat(notification.getContextData().get("userName")).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("사용자 알림 생성 테스트_성공_중첩된_Map_contextData")
    void createUserNotification_Success_NestedMapContextData() {
        // Given
        Map<String, Object> nestedData = Map.of(
                "user", Map.of("id", 1L, "name", "홍길동"),
                "order", Map.of("id", 12345L, "status", "completed"));

        // When
        UserNotification notification = userNotificationService.createUserNotification(
                testUser,
                testTemplate,
                nestedData);

        // Then
        assertThat(notification).isNotNull();
        assertThat(notification.getContextData()).isEqualTo(nestedData);
    }

    // Helper methods

    private User createTestUser() {
        return User.builder()
                .name("홍길동")
                .email("honggildong@example.com")
                .provider(OAuth2Provider.GOOGLE)
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
}
