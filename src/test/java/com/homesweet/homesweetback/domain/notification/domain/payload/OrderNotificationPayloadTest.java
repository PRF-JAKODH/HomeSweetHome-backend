package com.homesweet.homesweetback.domain.notification.domain.payload;

import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderNotificationPayload 테스트")
class OrderNotificationPayloadTest {
    
    @Nested
    @DisplayName("OrderCompletedPayload 테스트")
    class OrderCompletedPayloadTest {
        
        @Test
        @DisplayName("정상적인 Payload 생성 및 toMap() 동작")
        void testToMap() {
            // Given
            String userName = "홍길동";
            String orderId = "ORD123";
            
            OrderNotificationPayload.OrderCompletedPayload payload = 
                OrderNotificationPayload.OrderCompletedPayload.builder()
                    .userName(userName)
                    .orderId(orderId)
                    .build();
            
            // When
            Map<String, Object> result = payload.toMap();
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("userName")).isEqualTo(userName);
            assertThat(result.get("orderId")).isEqualTo(orderId);
        }
        
        @Test
        @DisplayName("validate() - 모든 필수 필드가 있을 때 성공")
        void testValidate_Success() {
            // Given
            OrderNotificationPayload.OrderCompletedPayload payload = 
                OrderNotificationPayload.OrderCompletedPayload.builder()
                    .userName("홍길동")
                    .orderId("ORD123")
                    .build();
            
            // When & Then
            payload.validate(NotificationEventType.ORDER_COMPLETED);
        }
        
        @Test
        @DisplayName("validate() - userName이 null일 때 예외 발생")
        void testValidate_UserNameNull() {
            // Given
            OrderNotificationPayload.OrderCompletedPayload payload = 
                OrderNotificationPayload.OrderCompletedPayload.builder()
                    .userName(null)
                    .orderId("ORD123")
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.ORDER_COMPLETED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userName is required");
        }
        
        @Test
        @DisplayName("validate() - orderId가 null일 때 예외 발생")
        void testValidate_OrderIdNull() {
            // Given
            OrderNotificationPayload.OrderCompletedPayload payload = 
                OrderNotificationPayload.OrderCompletedPayload.builder()
                    .userName("홍길동")
                    .orderId(null)
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.ORDER_COMPLETED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId is required");
        }
        
        @Test
        @DisplayName("@SupportsEventType 어노테이션 확인")
        void testSupportsEventTypeAnnotation() {
            // Given
            Class<?> clazz = OrderNotificationPayload.OrderCompletedPayload.class;
            
            // When
            SupportsEventType annotation = clazz.getAnnotation(SupportsEventType.class);
            
            // Then
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo(NotificationEventType.ORDER_COMPLETED);
        }
    }
    
    @Nested
    @DisplayName("OrderCancelledPayload 테스트")
    class OrderCancelledPayloadTest {
        
        @Test
        @DisplayName("정상적인 Payload 생성 및 toMap() 동작")
        void testToMap() {
            // Given
            String userName = "홍길동";
            String orderId = "ORD123";
            
            OrderNotificationPayload.OrderCancelledPayload payload = 
                OrderNotificationPayload.OrderCancelledPayload.builder()
                    .userName(userName)
                    .orderId(orderId)
                    .build();
            
            // When
            Map<String, Object> result = payload.toMap();
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("userName")).isEqualTo(userName);
            assertThat(result.get("orderId")).isEqualTo(orderId);
        }
        
        @Test
        @DisplayName("validate() - 모든 필수 필드가 있을 때 성공")
        void testValidate_Success() {
            // Given
            OrderNotificationPayload.OrderCancelledPayload payload = 
                OrderNotificationPayload.OrderCancelledPayload.builder()
                    .userName("홍길동")
                    .orderId("ORD123")
                    .build();
            
            // When & Then
            payload.validate(NotificationEventType.ORDER_CANCELLED);
        }
        
        @Test
        @DisplayName("validate() - userName이 null일 때 예외 발생")
        void testValidate_UserNameNull() {
            // Given
            OrderNotificationPayload.OrderCancelledPayload payload = 
                OrderNotificationPayload.OrderCancelledPayload.builder()
                    .userName(null)
                    .orderId("ORD123")
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.ORDER_CANCELLED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userName is required");
        }
        
        @Test
        @DisplayName("@SupportsEventType 어노테이션 확인")
        void testSupportsEventTypeAnnotation() {
            // Given
            Class<?> clazz = OrderNotificationPayload.OrderCancelledPayload.class;
            
            // When
            SupportsEventType annotation = clazz.getAnnotation(SupportsEventType.class);
            
            // Then
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo(NotificationEventType.ORDER_CANCELLED);
        }
    }
    
    @Nested
    @DisplayName("OrderShippedPayload 테스트")
    class OrderShippedPayloadTest {
        
        @Test
        @DisplayName("정상적인 Payload 생성 및 toMap() 동작")
        void testToMap() {
            // Given
            String userName = "홍길동";
            String orderId = "ORD123";
            
            OrderNotificationPayload.OrderShippedPayload payload = 
                OrderNotificationPayload.OrderShippedPayload.builder()
                    .userName(userName)
                    .orderId(orderId)
                    .build();
            
            // When
            Map<String, Object> result = payload.toMap();
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("userName")).isEqualTo(userName);
            assertThat(result.get("orderId")).isEqualTo(orderId);
        }
        
        @Test
        @DisplayName("validate() - 모든 필수 필드가 있을 때 성공")
        void testValidate_Success() {
            // Given
            OrderNotificationPayload.OrderShippedPayload payload = 
                OrderNotificationPayload.OrderShippedPayload.builder()
                    .userName("홍길동")
                    .orderId("ORD123")
                    .build();
            
            // When & Then
            payload.validate(NotificationEventType.ORDER_SHIPPED);
        }
        
        @Test
        @DisplayName("validate() - userName이 null일 때 예외 발생")
        void testValidate_UserNameNull() {
            // Given
            OrderNotificationPayload.OrderShippedPayload payload = 
                OrderNotificationPayload.OrderShippedPayload.builder()
                    .userName(null)
                    .orderId("ORD123")
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.ORDER_SHIPPED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userName is required");
        }
        
        @Test
        @DisplayName("@SupportsEventType 어노테이션 확인")
        void testSupportsEventTypeAnnotation() {
            // Given
            Class<?> clazz = OrderNotificationPayload.OrderShippedPayload.class;
            
            // When
            SupportsEventType annotation = clazz.getAnnotation(SupportsEventType.class);
            
            // Then
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo(NotificationEventType.ORDER_SHIPPED);
        }
    }
    
    @Nested
    @DisplayName("OrderDeliveredPayload 테스트")
    class OrderDeliveredPayloadTest {
        
        @Test
        @DisplayName("정상적인 Payload 생성 및 toMap() 동작")
        void testToMap() {
            // Given
            String userName = "홍길동";
            String orderId = "ORD123";
            
            OrderNotificationPayload.OrderDeliveredPayload payload = 
                OrderNotificationPayload.OrderDeliveredPayload.builder()
                    .userName(userName)
                    .orderId(orderId)
                    .build();
            
            // When
            Map<String, Object> result = payload.toMap();
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("userName")).isEqualTo(userName);
            assertThat(result.get("orderId")).isEqualTo(orderId);
        }
        
        @Test
        @DisplayName("validate() - 모든 필수 필드가 있을 때 성공")
        void testValidate_Success() {
            // Given
            OrderNotificationPayload.OrderDeliveredPayload payload = 
                OrderNotificationPayload.OrderDeliveredPayload.builder()
                    .userName("홍길동")
                    .orderId("ORD123")
                    .build();
            
            // When & Then
            payload.validate(NotificationEventType.ORDER_DELIVERED);
        }
        
        @Test
        @DisplayName("validate() - userName이 null일 때 예외 발생")
        void testValidate_UserNameNull() {
            // Given
            OrderNotificationPayload.OrderDeliveredPayload payload = 
                OrderNotificationPayload.OrderDeliveredPayload.builder()
                    .userName(null)
                    .orderId("ORD123")
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.ORDER_DELIVERED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userName is required");
        }
        
        @Test
        @DisplayName("@SupportsEventType 어노테이션 확인")
        void testSupportsEventTypeAnnotation() {
            // Given
            Class<?> clazz = OrderNotificationPayload.OrderDeliveredPayload.class;
            
            // When
            SupportsEventType annotation = clazz.getAnnotation(SupportsEventType.class);
            
            // Then
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo(NotificationEventType.ORDER_DELIVERED);
        }
    }
}
