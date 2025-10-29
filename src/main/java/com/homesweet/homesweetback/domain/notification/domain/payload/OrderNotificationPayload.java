package com.homesweet.homesweetback.domain.notification.domain.payload;

import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;

import lombok.Builder;

import java.util.Map;

/**
 * 어노테이션 기반 주문 관련 알림 Payload 클래스
 * 
 * @SupportsEventType 어노테이션을 사용하여 각 Payload가 지원하는 EventType을 명시합니다.
 * 
 * @author dogyungkim
 */
public class OrderNotificationPayload {
    
    /**
     * 주문 완료 알림 Payload
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     */
    @SupportsEventType(NotificationEventType.ORDER_COMPLETED)
    @Builder
    public static class OrderCompletedPayload extends NotificationPayload {
        private String userName;
        private String orderId;
        
        @Override
        public Map<String, Object> toMap() {
            return Map.of(
                "userName", userName != null ? userName : "",
                "orderId", orderId != null ? orderId : ""
            );
        }
        
        @Override
        protected void validateRequiredFields() {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for ORDER_COMPLETED notification");
            }
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("orderId is required for ORDER_COMPLETED notification");
            }
        }
    }
    
    /**
     * 주문 취소 알림 Payload
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     */
    @SupportsEventType(NotificationEventType.ORDER_CANCELLED)
    @Builder
    public static class OrderCancelledPayload extends NotificationPayload {
        private String userName;
        private String orderId;
        
        @Override
        public Map<String, Object> toMap() {
            return Map.of(
                "userName", userName != null ? userName : "",
                "orderId", orderId != null ? orderId : ""
            );
        }
        
        @Override
        protected void validateRequiredFields() {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for ORDER_CANCELLED notification");
            }
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("orderId is required for ORDER_CANCELLED notification");
            }
        }
    }
    
    /**
     * 배송 시작 알림 Payload
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     */
    @SupportsEventType(NotificationEventType.ORDER_SHIPPED)
    @Builder
    public static class OrderShippedPayload extends NotificationPayload {
        private String userName;
        private String orderId;
        
        @Override
        public Map<String, Object> toMap() {
            return Map.of(
                "userName", userName != null ? userName : "",
                "orderId", orderId != null ? orderId : ""
            );
        }
        
        @Override
        protected void validateRequiredFields() {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for ORDER_SHIPPED notification");
            }
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("orderId is required for ORDER_SHIPPED notification");
            }
        }
    }
    
    /**
     * 배송 완료 알림 Payload
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     */
    @SupportsEventType(NotificationEventType.ORDER_DELIVERED)
    @Builder
    public static class OrderDeliveredPayload extends NotificationPayload {
        private String userName;
        private String orderId;
        
        @Override
        public Map<String, Object> toMap() {
            return Map.of(
                "userName", userName != null ? userName : "",
                "orderId", orderId != null ? orderId : ""
            );
        }
        
        @Override
        protected void validateRequiredFields() {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for ORDER_DELIVERED notification");
            }
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("orderId is required for ORDER_DELIVERED notification");
            }
        }
    }
}