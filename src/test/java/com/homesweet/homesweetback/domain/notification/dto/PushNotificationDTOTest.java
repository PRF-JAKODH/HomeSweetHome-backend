package com.homesweet.homesweetback.domain.notification.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;

class PushNotificationDTOTest {

    @Test
    @DisplayName("PushNotificationDTO 빌더 테스트")
    void builder() {
        // Given
        Long notificationId = 1L;
        String title = "Title";
        String content = "Content";
        String redirectUrl = "http://example.com";
        Map<String, Object> contextData = Map.of("key", "value");
        boolean isRead = false;
        NotificationCategoryType categoryType = NotificationCategoryType.ORDER;
        LocalDateTime createdAt = LocalDateTime.now();

        // When
        PushNotificationDTO dto = PushNotificationDTO.builder()
                .notificationId(notificationId)
                .title(title)
                .content(content)
                .redirectUrl(redirectUrl)
                .contextData(contextData)
                .isRead(isRead)
                .categoryType(categoryType)
                .createdAt(createdAt)
                .build();

        // Then
        assertThat(dto.getNotificationId()).isEqualTo(notificationId);
        assertThat(dto.getTitle()).isEqualTo(title);
        assertThat(dto.getContent()).isEqualTo(content);
        assertThat(dto.getRedirectUrl()).isEqualTo(redirectUrl);
        assertThat(dto.getContextData()).isEqualTo(contextData);
        assertThat(dto.isRead()).isEqualTo(isRead);
        assertThat(dto.getCategoryType()).isEqualTo(categoryType);
        assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
    }
}
