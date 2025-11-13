package com.homesweet.homesweetback.domain.notification.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;

@DisplayName("SettlementNotification 테스트")
public class SettlementNotificationTest {

    @Test
    @DisplayName("SettlementCompleted 생성 테스트_성공")
    void testCreateSettlementCompleted() {
        // Given
        SettlementNotification.SettlementCompleted settlementCompleted = SettlementNotification.SettlementCompleted.builder()
            .userName("홍길동")
            .settlementId(12345L)
            .amount(100000L)
            .settlementName("1월 정산")
            .build();

        // Then
        assertThat(settlementCompleted.getUserName()).isEqualTo("홍길동");
        assertThat(settlementCompleted.getSettlementId()).isEqualTo(12345L);
        assertThat(settlementCompleted.getAmount()).isEqualTo(100000L);
        assertThat(settlementCompleted.getSettlementName()).isEqualTo("1월 정산");
        assertThat(settlementCompleted.getEventType()).isEqualTo(NotificationTemplateType.SETTLEMENT_COMPLETED);
    }

    @Test
    @DisplayName("SettlementCompleted 생성 테스트_실패_userName_null")
    void testCreateSettlementCompleted_Failure_UserNameNull() {
        // When & Then
        assertThatThrownBy(() -> SettlementNotification.SettlementCompleted.builder()
            .userName(null)
            .settlementId(12345L)
            .amount(100000L)
            .settlementName("1월 정산")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SettlementCompleted 생성 테스트_실패_amount_null")
    void testCreateSettlementCompleted_Failure_AmountNull() {
        // When & Then
        assertThatThrownBy(() -> SettlementNotification.SettlementCompleted.builder()
            .userName("홍길동")
            .settlementId(12345L)
            .amount(null)   
            .settlementName("1월 정산")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SettlementFailed 생성 테스트_성공")
    void testCreateSettlementFailed() {
        // Given
        SettlementNotification.SettlementFailed settlementFailed = SettlementNotification.SettlementFailed.builder()
            .userName("홍길동")
            .settlementId(12345L)
            .build();

        // Then
        assertThat(settlementFailed.getUserName()).isEqualTo("홍길동");
        assertThat(settlementFailed.getSettlementId()).isEqualTo(12345L);
        assertThat(settlementFailed.getEventType()).isEqualTo(NotificationTemplateType.SETTLEMENT_FAILED);
    }

    @Test
    @DisplayName("SettlementFailed 생성 테스트_실패_settlementId_null")
    void testCreateSettlementFailed_Failure_SettlementIdNull() {
        // When & Then
        assertThatThrownBy(() -> SettlementNotification.SettlementFailed.builder()
            .userName("홍길동")
            .settlementId(null)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }
}

