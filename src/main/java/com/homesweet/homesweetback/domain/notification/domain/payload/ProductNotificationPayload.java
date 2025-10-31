package com.homesweet.homesweetback.domain.notification.domain.payload;

import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;

import lombok.Builder;

import java.util.Map;

/**
 * 어노테이션 기반 상품 관련 알림 Payload 클래스
 * 
 * @SupportsEventType 어노테이션을 사용하여 각 Payload가 지원하는 EventType을 명시합니다.
 * 
 * @author dogyungkim
 */
public class ProductNotificationPayload {
    
    /**
     * 상품 승인 알림 Payload
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - productId: String - 상품 ID
     * - productName: String - 상품명
     */
    @SupportsEventType(NotificationEventType.PRODUCT_APPROVED)
    @Builder
    public static class ProductApprovedPayload extends NotificationPayload {
        private String userName;
        private String productId;
        private String productName;
        
        @Override
        public Map<String, Object> toMap() {
            return Map.of(
                "userName", userName != null ? userName : "",
                "productId", productId != null ? productId : "",
                "productName", productName != null ? productName : ""
            );
        }
        
        @Override
        protected void validateRequiredFields() {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for PRODUCT_APPROVED notification");
            }
            if (productId == null || productId.isBlank()) {
                throw new IllegalArgumentException("productId is required for PRODUCT_APPROVED notification");
            }
            if (productName == null || productName.isBlank()) {
                throw new IllegalArgumentException("productName is required for PRODUCT_APPROVED notification");
            }
        }
    }
    
    /**
     * 상품 거부 알림 Payload
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - productId: String - 상품 ID
     * - productName: String - 상품명
     */
    @SupportsEventType(NotificationEventType.PRODUCT_REJECTED)
    @Builder
    public static class ProductRejectedPayload extends NotificationPayload {
        private String userName;
        private String productId;
        private String productName;
        
        @Override
        public Map<String, Object> toMap() {
            return Map.of(
                "userName", userName != null ? userName : "",
                "productId", productId != null ? productId : "",
                "productName", productName != null ? productName : ""
            );
        }
        
        @Override
        protected void validateRequiredFields() {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for PRODUCT_REJECTED notification");
            }
            if (productId == null || productId.isBlank()) {
                throw new IllegalArgumentException("productId is required for PRODUCT_REJECTED notification");
            }
            if (productName == null || productName.isBlank()) {
                throw new IllegalArgumentException("productName is required for PRODUCT_REJECTED notification");
            }
        }
    }
    
    /**
     * 재고 부족 알림 Payload
     * 
     * 📋 필요한 contextData:
     * - productId: String - 상품 ID
     * - productName: String - 상품명
     * - currentStock: String - 현재 재고 수량
     */
    @SupportsEventType(NotificationEventType.PRODUCT_LOW_STOCK)
    @Builder
    public static class ProductLowStockPayload extends NotificationPayload {
        private String productId;
        private String productName;
        private String currentStock;
        
        @Override
        public Map<String, Object> toMap() {
            return Map.of(
                "productId", productId != null ? productId : "",
                "productName", productName != null ? productName : "",
                "currentStock", currentStock != null ? currentStock : ""
            );
        }
        
        @Override
        protected void validateRequiredFields() {
            if (productId == null || productId.isBlank()) {
                throw new IllegalArgumentException("productId is required for PRODUCT_LOW_STOCK notification");
            }
            if (productName == null || productName.isBlank()) {
                throw new IllegalArgumentException("productName is required for PRODUCT_LOW_STOCK notification");
            }
            if (currentStock == null || currentStock.isBlank()) {
                throw new IllegalArgumentException("currentStock is required for PRODUCT_LOW_STOCK notification");
            }
        }
    }
}