package com.homesweet.homesweetback.domain.notification.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.homesweet.homesweetback.domain.notification.domain.notification.CustomNotification;
import com.homesweet.homesweetback.domain.notification.exception.NotificationException;

@DisplayName("CustomNotificationEvent 테스트")
public class CustomNotificationEventTest {

    @Test
    @DisplayName("CustomNotificationEvent 생성 단일 사용자 테스트")
    void testCreateCustomNotificationEvent() {
        // Given
        String title = "주문 완료";
        String content = "주문이 완료되었습니다.";
        String redirectUrl = "/order/{orderId}";
        Map<String, Object> contextData = Map.of("orderId", "12345");
        CustomNotification notification = CustomNotification.builder()
                .title(title)
                .content(content)
                .redirectUrl(redirectUrl)
                .contextData(contextData)
                .build();
        CustomNotificationEvent event = new CustomNotificationEvent(1L, notification);
        // Then
        assertThat(event.userIds()).containsExactly(1L);
        assertThat(event.notification()).isEqualTo(notification);
    }

    @Test
    @DisplayName("CustomNotificationEvent 생성 다수 사용자 테스트")
    void testCreateCustomNotificationEvent_MultipleUsers() {
        // Given
        List<Long> userIds = List.of(1L, 2L, 3L);
        String title = "주문 완료";
        String content = "주문이 완료되었습니다.";
        String redirectUrl = "/order/{orderId}";
        Map<String, Object> contextData = Map.of("orderId", "12345");
        CustomNotification notification = CustomNotification.builder()
                .title(title)
                .content(content)
                .redirectUrl(redirectUrl)
                .contextData(contextData)
                .build();
        // When
        CustomNotificationEvent event = new CustomNotificationEvent(userIds, notification);

        // Then
        assertThat(event.userIds()).isEqualTo(userIds);
        assertThat(event.notification()).isEqualTo(notification);
    }

    @Test
    @DisplayName("CustomNotificationEvent 생성 유효성 검증 테스트")
    void testCreateCustomNotificationEvent_Validation() {
        // Given
        List<Long> userIds = List.of(1L, 2L, 3L);
        // When & Then
        // userIds is empty
        assertThatThrownBy(() -> new CustomNotificationEvent(List.of(), null))
                .isInstanceOf(NotificationException.class);
        // notification is null
        assertThatThrownBy(() -> new CustomNotificationEvent(userIds, null)).isInstanceOf(NotificationException.class);
    }
}
