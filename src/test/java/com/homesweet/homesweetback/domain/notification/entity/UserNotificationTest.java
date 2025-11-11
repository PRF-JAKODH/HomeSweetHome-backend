package com.homesweet.homesweetback.domain.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;
import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;
import com.homesweet.homesweetback.domain.notification.exception.NotificationException;

@DisplayName("UserNotification 테스트")
public class UserNotificationTest {
    
    @Test
    @DisplayName("UserNotification 생성 테스트")
    void createUserNotification_Success() {
        // Given
        User user = createTestUser();

        NotificationTemplate template = createTestNotificationTemplate();

        UserNotification userNotification = createTestUserNotification(user, template);

        // Then
        assertThat(userNotification.getUser()).isEqualTo(user);
        assertThat(userNotification.getTemplate()).isEqualTo(template);
        assertThat(userNotification.getContextData()).isEqualTo(Map.of("orderId", "123456789"));
    }

    @Test
    @DisplayName("UserNotification 읽음 표시 테스트")   
    void markAsRead_Success() {
        // Given
        UserNotification userNotification = createTestUserNotification(createTestUser(), createTestNotificationTemplate());
        // When
        userNotification.markAsRead();
        // Then
        assertThat(userNotification.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("UserNotification 삭제 표시 테스트")   
    void markAsDeleted_Success() {
        // Given
        UserNotification userNotification = createTestUserNotification(createTestUser(), createTestNotificationTemplate());
        // When
        userNotification.markAsDeleted();
        // Then
        assertThat(userNotification.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("UserNotification 생성 실패 테스트_UserId가 null인 경우")
    void createUserNotification_Fail() {
        // Given
        User user = null;
        NotificationTemplate template = createTestNotificationTemplate();
        // When
        assertThatThrownBy(() -> createTestUserNotification(user, template))
            .isInstanceOf(NotificationException.class)
            .hasMessageContaining(ErrorCode.NOTIFICATION_USER_ID_IS_NULL.getMessage());
    }

    @Test
    @DisplayName("UserNotification 생성 실패 테스트_ContextData가 null인 경우")
    void createUserNotification_Fail_ContextDataIsNull() {
        // Given
        User user = createTestUser();
        NotificationTemplate template = createTestNotificationTemplate();
        Map<String, Object> contextData = null;
        // When
        assertThatThrownBy(() -> UserNotification.builder()
                                                    .user(user)
                                                    .template(template)
                                                    .contextData(contextData)
                                                    .isRead(false)
                                                    .isDeleted(false)
                                                    .build())
            .isInstanceOf(NotificationException.class)
            .hasMessageContaining(ErrorCode.NOTIFICATION_CONTEXT_DATA_IS_NULL.getMessage());
    }

    private User createTestUser() {
        return User.builder()
            .id(1L)
            .name("홍길동")
            .email("honggildong@example.com")
            .build();
    }

    private NotificationTemplate createTestNotificationTemplate() {
        return NotificationTemplate.builder()
            .category(NotificationCategory.builder()
                .categoryType(NotificationCategoryType.ORDER)
                .build())
            .templateType(NotificationEventType.ORDER_COMPLETED)
            .title("주문 완료")
            .content("주문이 완료되었습니다.")
            .redirectUrl("app://order/{orderId}")
            .build();
    }   

    private UserNotification createTestUserNotification(User user, NotificationTemplate template) {
        return UserNotification.builder()
            .user(user)
            .template(template)
            .contextData(Map.of("orderId", "123456789"))
            .build();
    }
}
