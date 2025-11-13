package com.homesweet.homesweetback.domain.notification.domain.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;

import lombok.Builder;
import lombok.Getter;


/**
 * 주문 관련 알림 클래스
 * 
 * 주문 관련 알림들을 내부 클래스로 그룹화합니다.
 * 
 * @author dogyungkim
 */
public class OrderNotification {
    
    /**
     * 주문 완료 알림
     * 
     * 📋 필요한 필드:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     */
    @Getter
    public static class OrderCompleted implements TemplateNotification {
        private final String userName;
        private final String orderId;
        
        @JsonIgnore
        private final NotificationTemplateType eventType = NotificationTemplateType.ORDER_COMPLETED;
        
        @Builder
        public OrderCompleted(String userName, String orderId) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for ORDER_COMPLETED notification");
            }
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("orderId is required for ORDER_COMPLETED notification");
            }
            this.userName = userName;
            this.orderId = orderId;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
        
        @Override
        public void validate() {
            // 생성자에서 이미 검증됨
        }
    }
    
    /**
     * 주문 취소 알림
     * 
     * 📋 필요한 필드:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     */
    @Getter
    public static class OrderCancelled implements TemplateNotification {
        private final String userName;
        private final String orderId;
        
        @JsonIgnore
        private final NotificationTemplateType eventType = NotificationTemplateType.ORDER_CANCELLED;
        
        @Builder
        public OrderCancelled(String userName, String orderId) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for ORDER_CANCELLED notification");
            }
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("orderId is required for ORDER_CANCELLED notification");
            }
            this.userName = userName;
            this.orderId = orderId;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
        
        @Override
        public void validate() {
            // 생성자에서 이미 검증됨
        }
    }
    
    /**
     * 배송 시작 알림
     * 
     * 📋 필요한 필드:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     */
    @Getter
    public static class OrderShipped implements TemplateNotification {
        private final String userName;
        private final String orderId;
        
        @JsonIgnore
        private final NotificationTemplateType eventType = NotificationTemplateType.ORDER_SHIPPED;
        
        @Builder
        public OrderShipped(String userName, String orderId) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for ORDER_SHIPPED notification");
            }
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("orderId is required for ORDER_SHIPPED notification");
            }
            this.userName = userName;
            this.orderId = orderId;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
        
        @Override
        public void validate() {
            // 생성자에서 이미 검증됨
        }
    }
    
    /**
     * 배송 완료 알림
     * 
     * 📋 필요한 필드:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     */
    @Getter
    public static class OrderDelivered implements TemplateNotification {
        private final String userName;
        private final String orderId;
        
        @JsonIgnore
        private final NotificationTemplateType eventType = NotificationTemplateType.ORDER_DELIVERED;
        
        @Builder
        public OrderDelivered(String userName, String orderId) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for ORDER_DELIVERED notification");
            }
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("orderId is required for ORDER_DELIVERED notification");
            }
            this.userName = userName;
            this.orderId = orderId;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
        
        @Override
        public void validate() {
            // 생성자에서 이미 검증됨
        }
    }
}

