package com.homesweet.homesweetback.domain.notification.domain.payload;

import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SystemNotificationPayload 테스트")
class SystemNotificationPayloadTest {
    
    @Nested
    @DisplayName("SellerRegistrationCompletePayload 테스트")
    class SellerRegistrationCompletePayloadTest {
        
        @Test
        @DisplayName("정상적인 Payload 생성 및 toMap() 동작")
        void testToMap() {
            // Given
            String userName = "홍길동";
            
            SystemNotificationPayload.SellerRegistrationCompletePayload payload = 
                SystemNotificationPayload.SellerRegistrationCompletePayload.builder()
                    .userName(userName)
                    .build();
            
            // When
            Map<String, Object> result = payload.toMap();
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("userName")).isEqualTo(userName);
        }
        
        @Test
        @DisplayName("validate() - 모든 필수 필드가 있을 때 성공")
        void testValidate_Success() {
            // Given
            SystemNotificationPayload.SellerRegistrationCompletePayload payload = 
                SystemNotificationPayload.SellerRegistrationCompletePayload.builder()
                    .userName("홍길동")
                    .build();
            
            // When & Then
            payload.validate(NotificationEventType.SELLER_REGISTRATION_COMPLETE);
        }
        
        @Test
        @DisplayName("validate() - userName이 null일 때 예외 발생")
        void testValidate_UserNameNull() {
            // Given
            SystemNotificationPayload.SellerRegistrationCompletePayload payload = 
                SystemNotificationPayload.SellerRegistrationCompletePayload.builder()
                    .userName(null)
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.SELLER_REGISTRATION_COMPLETE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userName is required");
        }
        
        @Test
        @DisplayName("validate() - userName이 빈 문자열일 때 예외 발생")
        void testValidate_UserNameBlank() {
            // Given
            SystemNotificationPayload.SellerRegistrationCompletePayload payload = 
                SystemNotificationPayload.SellerRegistrationCompletePayload.builder()
                    .userName("")
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.SELLER_REGISTRATION_COMPLETE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userName is required");
        }
        
        @Test
        @DisplayName("@SupportsEventType 어노테이션 확인")
        void testSupportsEventTypeAnnotation() {
            // Given
            Class<?> clazz = SystemNotificationPayload.SellerRegistrationCompletePayload.class;
            
            // When
            SupportsEventType annotation = clazz.getAnnotation(SupportsEventType.class);
            
            // Then
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo(NotificationEventType.SELLER_REGISTRATION_COMPLETE);
        }
    }
}
