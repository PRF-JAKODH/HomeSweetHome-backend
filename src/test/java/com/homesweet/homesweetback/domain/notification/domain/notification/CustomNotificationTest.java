package com.homesweet.homesweetback.domain.notification.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;

import java.util.HashMap;
import java.util.Map;

@DisplayName("CustomNotification 테스트")
public class CustomNotificationTest {

    @Test
    @DisplayName("CustomNotification 생성 테스트_성공")
    void testCreateCustomNotification() {
        // Given
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("key1", "value1");
        contextData.put("key2", 123);

        CustomNotification customNotification = CustomNotification.builder()
            .title("커스텀 알림 제목")
            .content("커스텀 알림 내용")
            .redirectUrl("app://custom")
            .contextData(contextData)
            .build();

        // Then
        assertThat(customNotification.getTitle()).isEqualTo("커스텀 알림 제목");
        assertThat(customNotification.getContent()).isEqualTo("커스텀 알림 내용");
        assertThat(customNotification.getRedirectUrl()).isEqualTo("app://custom");
        assertThat(customNotification.getContextData()).isEqualTo(contextData);
        assertThat(customNotification.getEventType()).isEqualTo(NotificationTemplateType.CUSTOM);
    }


    @Test
    @DisplayName("CustomNotification 생성 테스트_실패_title_null")
    void testCreateCustomNotification_Failure_TitleNull() {
        // Given
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("key1", "value1");

        // When & Then
        assertThatThrownBy(() -> CustomNotification.builder()
            .title(null)
            .content("커스텀 알림 내용")
            .redirectUrl("app://custom")
            .contextData(contextData)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CustomNotification 생성 테스트_실패_content_blank")
    void testCreateCustomNotification_Failure_ContentBlank() {
        // Given
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("key1", "value1");

        // When & Then
        assertThatThrownBy(() -> CustomNotification.builder()
            .title("커스텀 알림 제목")
            .content("")
            .redirectUrl("app://custom")
            .contextData(contextData)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CustomNotification 생성 테스트_실패_redirectUrl_null")
    void testCreateCustomNotification_Failure_RedirectUrlNull() {
        // Given
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("key1", "value1");

        // When & Then
        assertThatThrownBy(() -> CustomNotification.builder()
            .title("커스텀 알림 제목")
            .content("커스텀 알림 내용")
            .redirectUrl(null)
            .contextData(contextData)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CustomNotification 생성 테스트_실패_contextData_null")
    void testCreateCustomNotification_Failure_ContextDataNull() {
        // When & Then
        assertThatThrownBy(() -> CustomNotification.builder()
            .title("커스텀 알림 제목")
            .content("커스텀 알림 내용")
            .redirectUrl("app://custom")
            .contextData(null)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    @DisplayName("CustomNotification toMap 테스트")
    void testToMap() {
        // Given
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("key1", "value1");
        contextData.put("key2", 123);

        CustomNotification customNotification = CustomNotification.builder()
            .title("커스텀 알림 제목")
            .content("커스텀 알림 내용")
            .redirectUrl("app://custom")
            .contextData(contextData)
            .build();

        // When
        Map<String, Object> result = customNotification.toMap();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(contextData);
        assertThat(result.get("key1")).isEqualTo("value1");
        assertThat(result.get("key2")).isEqualTo(123);
    }
}

