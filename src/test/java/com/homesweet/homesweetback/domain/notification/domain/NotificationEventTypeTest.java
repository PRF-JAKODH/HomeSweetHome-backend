package com.homesweet.homesweetback.domain.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NotificationEventType 테스트")
public class NotificationEventTypeTest {
    private final NotificationEventType eventType = NotificationEventType.ORDER_COMPLETED;

    @Test
    @DisplayName("NotificationEventType 테스트")
    void testNotificationEventType() {
        assertThat(eventType.getDescription()).isEqualTo("주문 완료");
        assertThat(eventType.getCategoryType()).isEqualTo(NotificationCategoryType.ORDER);
    }

    @Test
    @DisplayName("NotificationEventType fromCode 테스트_성공")
    void testNotificationEventTypeFromCode_Success() {
        assertThat(NotificationEventType.fromCode("ORDER_COMPLETED")).isEqualTo(eventType);
    }

    @Test
    @DisplayName("NotificationEventType fromCode 테스트_실패")
    void testNotificationEventTypeFromCode_Failure() {
        assertThatThrownBy(() -> NotificationEventType.fromCode("INVALID_EVENT_TYPE")).isInstanceOf(IllegalArgumentException.class);
    }
}
