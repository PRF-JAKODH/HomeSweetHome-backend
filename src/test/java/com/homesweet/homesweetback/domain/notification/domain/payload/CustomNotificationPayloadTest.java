package com.homesweet.homesweetback.domain.notification.domain.payload;

import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CustomNotificationPayload 테스트")
class CustomNotificationPayloadTest {
    
    @Test
    @DisplayName("정상적인 Payload 생성 및 toMap() 동작")
    void testToMap() {
        // Given
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("key1", "value1");
        contextData.put("key2", 123);
        contextData.put("key3", true);
        
        CustomNotificationPayload payload = new CustomNotificationPayload(contextData);
        
        // When
        Map<String, Object> result = payload.toMap();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(contextData);
        assertThat(result.get("key1")).isEqualTo("value1");
        assertThat(result.get("key2")).isEqualTo(123);
        assertThat(result.get("key3")).isEqualTo(true);
    }
    
    @Test
    @DisplayName("validate() - contextData가 있을 때 성공")
    void testValidate_Success() {
        // Given
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("key", "value");
        
        CustomNotificationPayload payload = new CustomNotificationPayload(contextData);
        
        // When & Then
        payload.validate(NotificationEventType.CUSTOM);
    }
    
    @Test
    @DisplayName("validate() - contextData가 null일 때 예외 발생")
    void testValidate_ContextDataNull() {
        // Given
        CustomNotificationPayload payload = new CustomNotificationPayload(null);
        
        // When & Then
        assertThatThrownBy(() -> payload.validate(NotificationEventType.CUSTOM))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("contextData is required");
    }
    
    @Test
    @DisplayName("validate() - contextData가 비어있을 때 예외 발생")
    void testValidate_ContextDataEmpty() {
        // Given
        CustomNotificationPayload payload = new CustomNotificationPayload(new HashMap<>());
        
        // When & Then
        assertThatThrownBy(() -> payload.validate(NotificationEventType.CUSTOM))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("contextData is required");
    }
    
    @Test
    @DisplayName("@SupportsEventType 어노테이션 확인")
    void testSupportsEventTypeAnnotation() {
        // Given
        Class<?> clazz = CustomNotificationPayload.class;
        
        // When
        SupportsEventType annotation = clazz.getAnnotation(SupportsEventType.class);
        
        // Then
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(NotificationEventType.CUSTOM);
    }
    
    @Test
    @DisplayName("toMap()은 원본 contextData와 동일한 참조를 반환")
    void testToMap_ReturnsSameReference() {
        // Given
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("key", "value");
        
        CustomNotificationPayload payload = new CustomNotificationPayload(contextData);
        
        // When
        Map<String, Object> result = payload.toMap();
        
        // Then
        assertThat(result).isSameAs(contextData);
    }
    
    @Test
    @DisplayName("다양한 타입의 데이터를 contextData에 저장 가능")
    void testToMap_VariousDataTypes() {
        // Given
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("string", "text");
        contextData.put("integer", 42);
        contextData.put("long", 100L);
        contextData.put("boolean", true);
        contextData.put("double", 3.14);
        
        CustomNotificationPayload payload = new CustomNotificationPayload(contextData);
        
        // When
        Map<String, Object> result = payload.toMap();
        
        // Then
        assertThat(result.get("string")).isEqualTo("text");
        assertThat(result.get("integer")).isEqualTo(42);
        assertThat(result.get("long")).isEqualTo(100L);
        assertThat(result.get("boolean")).isEqualTo(true);
        assertThat(result.get("double")).isEqualTo(3.14);
    }
}
