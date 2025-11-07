package com.homesweet.homesweetback.domain.notification.service;

import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;
import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;
import com.homesweet.homesweetback.domain.notification.domain.payload.OrderNotificationPayload;
import com.homesweet.homesweetback.domain.notification.entity.NotificationCategory;
import com.homesweet.homesweetback.domain.notification.entity.NotificationTemplate;
import com.homesweet.homesweetback.domain.notification.entity.UserNotification;
import com.homesweet.homesweetback.domain.notification.exception.NotificationException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.notification.repository.NotificationTemplateRepository;
import com.homesweet.homesweetback.domain.notification.repository.UserNotificationRepository;
import com.homesweet.homesweetback.domain.notification.repository.NotificationCategoryRepository;
import com.homesweet.homesweetback.domain.notification.service.impl.NotificationSendServiceImpl;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

/** 
 * Notification Service Impl 단위 테스트
 * 
 * @author dogyungkim
 */
@ActiveProfiles("test")
@SpringBootTest
@DisplayName("NotificationSendServiceImpl 테스트")
public class NotificationSendServiceImplTest {
    
    @Autowired
    private NotificationSendServiceImpl notificationSendService;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;

    @Autowired
    private NotificationCategoryRepository notificationCategoryRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    private User testUser;
    private NotificationTemplate testTemplate;
    private NotificationCategory testCategory;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(createTestUser());
        testCategory = notificationCategoryRepository.save(NotificationCategory.builder()
        .categoryType(NotificationCategoryType.ORDER)
        .build());
        testTemplate = notificationTemplateRepository.save(createTestNotificationTemplate());

    }

    @Test
    @DisplayName("UserNotification 생성 테스트_성공")
    void createUserNotification_Success() {
        // Given
        Map<String, Object> contextData = Map.of("orderId", "12345");
        // When
        UserNotification userNotification = notificationSendService.createAndSaveUserNotification(testUser.getId(), testTemplate, contextData);
        // Then
        assertThat(userNotification.getId()).isNotNull();
        assertThat(userNotification.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(userNotification.getTemplate().getId()).isEqualTo(testTemplate.getId());
        assertThat(userNotification.getContextData()).isEqualTo(contextData);
        assertThat(userNotification.getIsRead()).isFalse();
        assertThat(userNotification.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("UserNotification 저장 테스트_성공")
    void saveUserNotification_Success() {
        // Given
        UserNotification userNotification = notificationSendService.createAndSaveUserNotification(testUser.getId(), testTemplate, Map.of("orderId", "12345"));
        // When
        UserNotification savedUserNotification = userNotificationRepository.findById(userNotification.getId()).orElseThrow();
        // Then
        assertThat(savedUserNotification.getId()).isNotNull();
        assertThat(savedUserNotification.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(savedUserNotification.getTemplate().getId()).isEqualTo(testTemplate.getId());
        assertThat(savedUserNotification.getContextData()).isEqualTo(Map.of("orderId", "12345"));
        assertThat(savedUserNotification.getIsRead()).isFalse();
        assertThat(savedUserNotification.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("UserNotification 생성 테스트_User가 존재하지 않는 경우_실패")
    void createUserNotification_UserNotFound() {
        // Given
        Long userId = 100L;
        Map<String, Object> contextData = Map.of("orderId", "12345");
        // When
        // Then
        assertThatThrownBy(() -> notificationSendService.createAndSaveUserNotification(userId, testTemplate, contextData))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("UserNotification 생성 테스트_Payload에 ContextData가 없는 경우_실패")
    void createUserNotification_PayloadContextDataNotFound() {
        // Given
        // When
        // Then
        assertThatThrownBy(() -> notificationSendService.createAndSaveUserNotification(testUser.getId(), testTemplate, null))
            .isInstanceOf(NotificationException.class)
            .hasMessageContaining(ErrorCode.NOTIFICATION_CONTEXT_DATA_IS_NULL.getMessage());
    }


    private User createTestUser() {
        return User.builder()
            .name("홍길동")
            .email("honggildong@example.com")
            .provider(OAuth2Provider.KAKAO)
            .providerId("123456789")
            .role(UserRole.USER)
            .build();
    }   

    private NotificationTemplate createTestNotificationTemplate() {
        return NotificationTemplate.builder()
            .category(testCategory)
            .templateType(NotificationEventType.ORDER_COMPLETED)
            .title("주문 완료")
            .content("주문이 완료되었습니다.")
            .redirectUrl("/order/{orderId}")
            .build();
    }   
}
