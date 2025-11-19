package com.homesweet.homesweetback.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
import com.homesweet.homesweetback.domain.notification.repository.UserNotificationRepository;
import com.homesweet.homesweetback.domain.notification.service.impl.UserNotificationService;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UserNotificationService 테스트")
public class UserNotificationServiceTest {

    @Autowired
    private UserNotificationService userNotificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

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

    @AfterEach
    void tearDown() {
        userNotificationRepository.deleteAll();
        notificationTemplateRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("사용자 알림 생성 및 저장 테스트_성공")
    void createAndSaveUserNotification_Success() {
        // Given
        Map<String, Object> contextData = Map.of("orderId", 12345L, "userName", "홍길동");

        // When
        UserNotification savedNotification = userNotificationService.createAndSaveUserNotification(
                testUser.getId(),
                testTemplate,
                contextData
        );

        // Then
        assertThat(savedNotification.getId()).isNotNull();
        assertThat(savedNotification.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(savedNotification.getTemplate()).isEqualTo(testTemplate);
        assertThat(savedNotification.getContextData()).isEqualTo(contextData);
        assertThat(savedNotification.getIsRead()).isFalse();
        assertThat(savedNotification.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("사용자 알림 생성 및 저장 테스트_실패_존재하지 않는 사용자")
    void createAndSaveUserNotification_Failure_UserNotFound() {
        // Given
        Long nonExistentUserId = 999999L;
        Map<String, Object> contextData = Map.of("orderId", 12345L);

        // When & Then
        assertThatThrownBy(() -> {
            userNotificationService.createAndSaveUserNotification(
                    nonExistentUserId,
                    testTemplate,
                    contextData
            );
        }).isInstanceOf(Exception.class); // 외래키 제약조건 위반 또는 EntityNotFoundException
    }

    @Test
    @DisplayName("커스텀 알림 템플릿 생성 및 저장 테스트_성공")
    void createAndSaveCustomNotificationTemplate_Success() {
        // Given
        String title = "커스텀 알림 제목";
        String content = "커스텀 알림 내용입니다.";
        String redirectUrl = "app://custom/notification";

        // When
        NotificationTemplate savedTemplate = userNotificationService.createAndSaveCustomNotificationTemplate(
                title,
                content,
                redirectUrl
        );

        // Then
        assertThat(savedTemplate.getId()).isNotNull();
        assertThat(savedTemplate.getTitle()).isEqualTo(title);
        assertThat(savedTemplate.getContent()).isEqualTo(content);
        assertThat(savedTemplate.getRedirectUrl()).isEqualTo(redirectUrl);
        assertThat(savedTemplate.getTemplateType()).isEqualTo(NotificationTemplateType.CUSTOM);
        assertThat(savedTemplate.getCategory().getId()).isEqualTo(NotificationCategoryType.CUSTOM.getCategoryId());
    }

    @Test
    @DisplayName("알림 템플릿 조회 테스트_성공")
    void getNotificationTemplate_Success() {
        // Given
        NotificationTemplateType eventType = NotificationTemplateType.ORDER_COMPLETED;

        // When
        NotificationTemplate foundTemplate = userNotificationService.getNotificationTemplate(eventType);

        // Then
        assertThat(foundTemplate).isNotNull();
        assertThat(foundTemplate.getTemplateType()).isEqualTo(eventType);
        assertThat(foundTemplate.getTitle()).isNotNull();
        assertThat(foundTemplate.getContent()).isNotNull();
        assertThat(foundTemplate.getRedirectUrl()).isNotNull();
    }

    @Test
    @DisplayName("알림 템플릿 조회 테스트_다양한 템플릿 타입")
    void getNotificationTemplate_VariousTemplateTypes() {
        // Given & When & Then
        // 다양한 템플릿 타입 조회 테스트
        NotificationTemplate orderTemplate = userNotificationService.getNotificationTemplate(NotificationTemplateType.ORDER_COMPLETED);
        assertThat(orderTemplate).isNotNull();
        assertThat(orderTemplate.getTemplateType()).isEqualTo(NotificationTemplateType.ORDER_COMPLETED);
    }

    @Test
    @DisplayName("사용자 알림 생성 및 저장 테스트_다양한 컨텍스트 데이터")
    void createAndSaveUserNotification_WithVariousContextData() {
        // Given
        Map<String, Object> complexContextData = Map.of(
                "orderId", 12345L,
                "userName", "홍길동",
                "amount", 50000,
                "status", "completed"
        );

        // When
        UserNotification savedNotification = userNotificationService.createAndSaveUserNotification(
                testUser.getId(),
                testTemplate,
                complexContextData
        );

        // Then
        assertThat(savedNotification.getId()).isNotNull();
        assertThat(savedNotification.getContextData()).isEqualTo(complexContextData);
        assertThat(savedNotification.getContextData().get("orderId")).isEqualTo(12345L);
        assertThat(savedNotification.getContextData().get("userName")).isEqualTo("홍길동");
        assertThat(savedNotification.getContextData().get("amount")).isEqualTo(50000);
        assertThat(savedNotification.getContextData().get("status")).isEqualTo("completed");
    }

    @Test
    @DisplayName("커스텀 알림 템플릿 생성 및 저장 테스트_긴 제목과 내용")
    void createAndSaveCustomNotificationTemplate_WithLongContent() {
        // Given
        String longTitle = "매우 긴 커스텀 알림 제목입니다. 이 제목은 50자를 넘지 않아야 합니다.";
        String longContent = "매우 긴 커스텀 알림 내용입니다. 이 내용은 200자를 넘지 않아야 합니다. " +
                "실제로는 더 긴 내용이 들어갈 수 있지만, 데이터베이스 스키마에 따라 제한이 있을 수 있습니다.";
        String redirectUrl = "app://custom/notification/very/long/path";

        // When
        NotificationTemplate savedTemplate = userNotificationService.createAndSaveCustomNotificationTemplate(
                longTitle,
                longContent,
                redirectUrl
        );

        // Then
        assertThat(savedTemplate.getId()).isNotNull();
        assertThat(savedTemplate.getTitle()).isEqualTo(longTitle);
        assertThat(savedTemplate.getContent()).isEqualTo(longContent);
        assertThat(savedTemplate.getRedirectUrl()).isEqualTo(redirectUrl);
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

