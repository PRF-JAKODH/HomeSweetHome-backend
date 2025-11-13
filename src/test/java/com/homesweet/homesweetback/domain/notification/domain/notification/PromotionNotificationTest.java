package com.homesweet.homesweetback.domain.notification.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;

@DisplayName("PromotionNotification 테스트")
public class PromotionNotificationTest {

    @Test
    @DisplayName("PromotionStart 생성 테스트_성공")
    void testCreatePromotionStart() {
        // Given
        PromotionNotification.PromotionStart promotionStart = PromotionNotification.PromotionStart.builder()
            .promotionName("신년 프로모션")
            .build();

        // Then
        assertThat(promotionStart.getPromotionName()).isEqualTo("신년 프로모션");
        assertThat(promotionStart.getEventType()).isEqualTo(NotificationTemplateType.PROMOTION_START);
    }

    @Test
    @DisplayName("PromotionStart 생성 테스트_실패_promotionName_null")
    void testCreatePromotionStart_Failure_PromotionNameNull() {
        // When & Then
        assertThatThrownBy(() -> PromotionNotification.PromotionStart.builder()
            .promotionName(null)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("PromotionEnd 생성 테스트_성공")
    void testCreatePromotionEnd() {
        // Given
        PromotionNotification.PromotionEnd promotionEnd = PromotionNotification.PromotionEnd.builder()
            .promotionName("신년 프로모션")
            .build();

        // Then
        assertThat(promotionEnd.getPromotionName()).isEqualTo("신년 프로모션");
        assertThat(promotionEnd.getEventType()).isEqualTo(NotificationTemplateType.PROMOTION_END);
    }

    @Test
    @DisplayName("PromotionEnd 생성 테스트_실패_promotionName_blank")
    void testCreatePromotionEnd_Failure_PromotionNameBlank() {
        // When & Then
        assertThatThrownBy(() -> PromotionNotification.PromotionEnd.builder()
            .promotionName("")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }
}

