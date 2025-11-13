package com.homesweet.homesweetback.domain.notification.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;

@DisplayName("PaymentNotification 테스트")
public class PaymentNotificationTest {

    @Test
    @DisplayName("PaymentSuccess 생성 테스트_성공")
    void testCreatePaymentSuccess() {
        // Given
        PaymentNotification.PaymentSuccess paymentSuccess = PaymentNotification.PaymentSuccess.builder()
            .userName("홍길동")
            .amount("50000")
            .build();

        // Then
        assertThat(paymentSuccess.getUserName()).isEqualTo("홍길동");
        assertThat(paymentSuccess.getAmount()).isEqualTo("50000");
        assertThat(paymentSuccess.getEventType()).isEqualTo(NotificationTemplateType.PAYMENT_SUCCESS);
    }

    @Test
    @DisplayName("PaymentSuccess 생성 테스트_실패_userName_null")
    void testCreatePaymentSuccess_Failure_UserNameNull() {
        // When & Then
        assertThatThrownBy(() -> PaymentNotification.PaymentSuccess.builder()
            .userName(null)
            .amount("50000")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("PaymentSuccess 생성 테스트_실패_amount_blank")
    void testCreatePaymentSuccess_Failure_AmountBlank() {
        // When & Then
        assertThatThrownBy(() -> PaymentNotification.PaymentSuccess.builder()
            .userName("홍길동")
            .amount("")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("PaymentFailed 생성 테스트_성공")
    void testCreatePaymentFailed() {
        // Given
        PaymentNotification.PaymentFailed paymentFailed = PaymentNotification.PaymentFailed.builder()
            .userName("홍길동")
            .orderId("order-123")
            .build();

        // Then
        assertThat(paymentFailed.getUserName()).isEqualTo("홍길동");
        assertThat(paymentFailed.getOrderId()).isEqualTo("order-123");
        assertThat(paymentFailed.getEventType()).isEqualTo(NotificationTemplateType.PAYMENT_FAILED);
    }

    @Test
    @DisplayName("PaymentFailed 생성 테스트_실패_orderId_blank")
    void testCreatePaymentFailed_Failure_OrderIdBlank() {
        // When & Then
        assertThatThrownBy(() -> PaymentNotification.PaymentFailed.builder()
            .userName("홍길동")
            .orderId("")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("PaymentRefunded 생성 테스트_성공")
    void testCreatePaymentRefunded() {
        // Given
        PaymentNotification.PaymentRefunded paymentRefunded = PaymentNotification.PaymentRefunded.builder()
            .userName("홍길동")
            .amount("50000")
            .build();

        // Then
        assertThat(paymentRefunded.getUserName()).isEqualTo("홍길동");
        assertThat(paymentRefunded.getAmount()).isEqualTo("50000");
        assertThat(paymentRefunded.getEventType()).isEqualTo(NotificationTemplateType.PAYMENT_REFUNDED);
    }

    @Test
    @DisplayName("PaymentRefunded 생성 테스트_실패_amount_null")
    void testCreatePaymentRefunded_Failure_AmountNull() {
        // When & Then
        assertThatThrownBy(() -> PaymentNotification.PaymentRefunded.builder()
            .userName("홍길동")
            .amount(null)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }
}

