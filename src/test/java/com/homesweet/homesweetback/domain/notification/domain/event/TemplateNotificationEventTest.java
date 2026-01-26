package com.homesweet.homesweetback.domain.notification.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;
import com.homesweet.homesweetback.domain.notification.domain.notification.OrderNotification;

@DisplayName("TemplateNotificationEvent 테스트")
public class TemplateNotificationEventTest {
    
    @Test
    @DisplayName("TemplateNotificationEvent 생성 테스트_단일 사용자")
    void testCreateTemplateNotificationEvent_SingleUser() {
        // Given
        TemplateNotificationEvent event = new TemplateNotificationEvent(1L, OrderNotification.OrderCompleted.builder()
            .userName("홍길동")
            .orderId(12345L)
            .build());

        // Then
        assertThat(event.userIds()).containsExactly(1L);
        assertThat(event.notification()).isNotNull();
        assertThat(event.notification().getEventType()).isEqualTo(NotificationTemplateType.ORDER_COMPLETED);
    }

    @Test
    @DisplayName("TemplateNotificationEvent 생성 테스트_다수 사용자")
    void testCreateTemplateNotificationEvent_MultipleUsers() {
        // Given
        TemplateNotificationEvent event = new TemplateNotificationEvent(List.of(1L, 2L, 3L), OrderNotification.OrderCompleted.builder()
            .userName("홍길동")
            .orderId(12345L)
            .build());

        // Then
        assertThat(event.userIds()).containsExactly(1L, 2L, 3L);
        assertThat(event.notification()).isNotNull();
        assertThat(event.notification().getEventType()).isEqualTo(NotificationTemplateType.ORDER_COMPLETED);
    }

    @Test
    @DisplayName("TemplateNotificationEvent 생성 테스트_유효성 검증")
    void testCreateTemplateNotificationEvent_Validation() {
        // Given
        // When & Then
        assertThatThrownBy(() -> new TemplateNotificationEvent(List.of(1L), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TemplateNotificationEvent(List.of(), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TemplateNotificationEvent(1L, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
