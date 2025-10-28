package com.homesweet.homesweetback.domain.notification.domain.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 주문 관련 알림 Payload 클래스
 */
public class OrderNotificationPayload {
    
    /**
     * 주문 완료 알림 Payload
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     */
    @Builder
    public static class OrderCompletedPayload implements NotificationPayload {
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
        public void validate() {
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
    @Builder
    public static class OrderCancelledPayload implements NotificationPayload {
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
        public void validate() {
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
    @Builder
    public static class OrderShippedPayload implements NotificationPayload {
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
        public void validate() {
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
    @Builder
    public static class OrderDeliveredPayload implements NotificationPayload {
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
        public void validate() {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for ORDER_DELIVERED notification");
            }
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("orderId is required for ORDER_DELIVERED notification");
            }
        }
    }
}
