package com.homesweet.homesweetback.domain.notification.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;

@DisplayName("OrderNotification 테스트")
public class OrderNotificationTest {

    @Test
    @DisplayName("OrderCompleted 생성 테스트_성공")
    void testCreateOrderCompleted() {
        // Given
        OrderNotification.OrderCompleted orderCompleted = OrderNotification.OrderCompleted.builder()
            .userName("홍길동")
            .orderId(12345L)
            .build();

        // Then
        assertThat(orderCompleted.getUserName()).isEqualTo("홍길동");
        assertThat(orderCompleted.getOrderId()).isEqualTo(12345L);
        assertThat(orderCompleted.getEventType()).isEqualTo(NotificationTemplateType.ORDER_COMPLETED);
    }

    @Test
    @DisplayName("OrderCompleted 생성 테스트_실패_userName_null")
    void testCreateOrderCompleted_Failure_UserNameNull() {
        // When & Then
        assertThatThrownBy(() -> OrderNotification.OrderCompleted.builder()
            .userName(null)
            .orderId(12345L)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("OrderCompleted 생성 테스트_실패_orderId_blank")
    void testCreateOrderCompleted_Failure_OrderIdBlank() {
        // When & Then
        assertThatThrownBy(() -> OrderNotification.OrderCompleted.builder()
            .userName("홍길동")
            .orderId(null)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("OrderCancelled 생성 테스트_성공")
    void testCreateOrderCancelled() {
        // Given
        OrderNotification.OrderCancelled orderCancelled = OrderNotification.OrderCancelled.builder()
            .userName("홍길동")
            .orderId(12345L)
            .build();

        // Then
        assertThat(orderCancelled.getUserName()).isEqualTo("홍길동");
        assertThat(orderCancelled.getOrderId()).isEqualTo(12345L);
        assertThat(orderCancelled.getEventType()).isEqualTo(NotificationTemplateType.ORDER_CANCELLED);
    }

    @Test
    @DisplayName("OrderCancelled 생성 테스트_실패_orderId_null")
    void testCreateOrderCancelled_Failure_OrderIdNull() {
        // When & Then
        assertThatThrownBy(() -> OrderNotification.OrderCancelled.builder()
            .userName("홍길동")
            .orderId(null)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("OrderShipped 생성 테스트_성공")
    void testCreateOrderShipped() {
        // Given
        OrderNotification.OrderShipped orderShipped = OrderNotification.OrderShipped.builder()
            .userName("홍길동")
            .orderId(12345L)
            .build();

        // Then
        assertThat(orderShipped.getUserName()).isEqualTo("홍길동");
        assertThat(orderShipped.getOrderId()).isEqualTo(12345L);
        assertThat(orderShipped.getEventType()).isEqualTo(NotificationTemplateType.ORDER_SHIPPED);
    }

    @Test
    @DisplayName("OrderShipped 생성 테스트_실패_userName_null")
    void testCreateOrderShipped_Failure_UserNameNull() {
        // When & Then
        assertThatThrownBy(() -> OrderNotification.OrderShipped.builder()
            .userName(null)
            .orderId(12345L)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("OrderDelivered 생성 테스트_성공")
    void testCreateOrderDelivered() {
        // Given
        OrderNotification.OrderDelivered orderDelivered = OrderNotification.OrderDelivered.builder()
            .userName("홍길동")
            .orderId(12345L)
            .build();

        // Then
        assertThat(orderDelivered.getUserName()).isEqualTo("홍길동");
        assertThat(orderDelivered.getOrderId()).isEqualTo(12345L);
        assertThat(orderDelivered.getEventType()).isEqualTo(NotificationTemplateType.ORDER_DELIVERED);
    }

    @Test
    @DisplayName("OrderDelivered 생성 테스트_실패_orderId_null")
    void testCreateOrderDelivered_Failure_OrderIdBlank() {
        // When & Then
        assertThatThrownBy(() -> OrderNotification.OrderDelivered.builder()
            .userName("홍길동")
            .orderId(null)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }
}
