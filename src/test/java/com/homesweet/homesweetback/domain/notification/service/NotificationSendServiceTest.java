package com.homesweet.homesweetback.domain.notification.service;

import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;
import com.homesweet.homesweetback.domain.notification.domain.payload.NotificationPayload;
import com.homesweet.homesweetback.domain.notification.domain.payload.OrderNotificationPayload;
import com.homesweet.homesweetback.domain.notification.domain.payload.ProductNotificationPayload;
import com.homesweet.homesweetback.domain.notification.exception.NotificationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;


/**
 * 어노테이션 기반 Notification Service 테스트
 * 
 * @author dogyungkim
 */
@ExtendWith(MockitoExtension.class)
class NotificationSendServiceTest {
    
    @Mock
    private NotificationSendService notificationSendService;
    
    @Test
    @DisplayName("올바른 EventType과 Payload 조합 - 성공")
    void correctEventTypeAndPayload_Success() {
        // Given
        Long userId = 1L;
        NotificationPayload payload = OrderNotificationPayload.OrderCompletedPayload.builder()
            .userName("홍길동")
            .orderId("12345")
            .build();
        
        // When & Then
        assertDoesNotThrow(() -> {
            payload.validate(NotificationEventType.ORDER_COMPLETED);
        });
    }
    
    @Test
    @DisplayName("잘못된 EventType과 Payload 조합 - 실패")
    void incorrectEventTypeAndPayload_Failure() {
        // Given
        NotificationPayload payload = OrderNotificationPayload.OrderCompletedPayload.builder()
            .userName("홍길동")
            .orderId("12345")
            .build();
        
        // When & Then
        NotificationException exception = assertThrows(NotificationException.class, () -> {
            payload.validate(NotificationEventType.PRODUCT_APPROVED);
        });
        
        assertNotNull(exception);
    }
    
    @Test
    @DisplayName("필수 필드 누락 - 실패")
    void missingRequiredField_Failure() {
        // Given - userName 누락
        NotificationPayload payload = OrderNotificationPayload.OrderCompletedPayload.builder()
            .orderId("12345")
            // userName 누락!
            .build();
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            payload.validate(NotificationEventType.ORDER_COMPLETED);
        });
        
        assertTrue(exception.getMessage().contains("userName is required"));
    }
    
    @Test
    @DisplayName("상품 관련 Payload 검증 - 성공")
    void productPayloadValidation_Success() {
        // Given
        NotificationPayload productApprovedPayload = ProductNotificationPayload.ProductApprovedPayload.builder()
            .userName("홍길동")
            .productId("P001")
            .productName("아이폰")
            .build();
        
        NotificationPayload productRejectedPayload = ProductNotificationPayload.ProductRejectedPayload.builder()
            .userName("홍길동")
            .productId("P001")
            .productName("아이폰")
            .build();
        
        NotificationPayload productLowStockPayload = ProductNotificationPayload.ProductLowStockPayload.builder()
            .userName("홍길동")
            .productId("P001")
            .productName("아이폰")
            .currentStock("5")
            .build();
        
        // When & Then
        assertDoesNotThrow(() -> {
            productApprovedPayload.validate(NotificationEventType.PRODUCT_APPROVED);
        });
        
        assertDoesNotThrow(() -> {
            productRejectedPayload.validate(NotificationEventType.PRODUCT_REJECTED);
        });
        
        assertDoesNotThrow(() -> {
            productLowStockPayload.validate(NotificationEventType.PRODUCT_LOW_STOCK);
        });
    }
    
    @Test
    @DisplayName("상품 관련 Payload 잘못된 EventType - 실패")
    void productPayloadWrongEventType_Failure() {
        // Given
        NotificationPayload productPayload = ProductNotificationPayload.ProductApprovedPayload.builder()
            .userName("홍길동")
            .productId("P001")
            .productName("아이폰")
            .build();
        
        // When & Then - 주문 관련 EventType 사용
        assertThrows(NotificationException.class, () -> {
            productPayload.validate(NotificationEventType.ORDER_COMPLETED);
        });
    }
    
    @Test
    @DisplayName("Payload toMap() 변환 테스트")
    void payloadToMapTest() {
        // Given
        NotificationPayload payload = OrderNotificationPayload.OrderCompletedPayload.builder()
            .userName("홍길동")
            .orderId("12345")
            .build();
        
        // When
        var result = payload.toMap();
        
        // Then
        assertNotNull(result);
        assertEquals("홍길동", result.get("userName"));
        assertEquals("12345", result.get("orderId"));
    }
    
    @Test
    @DisplayName("어노테이션이 없는 Payload 클래스 - 실패")
    void payloadWithoutAnnotation_Failure() {
        // Given - 어노테이션이 없는 Payload 클래스
        NotificationPayload payloadWithoutAnnotation = new NotificationPayload() {
            @Override
            public java.util.Map<String, Object> toMap() {
                return java.util.Map.of();
            }
        };
        
        // When & Then
        assertThrows(NotificationException.class, () -> {
            payloadWithoutAnnotation.validate(NotificationEventType.ORDER_COMPLETED);
        });
    }
}
