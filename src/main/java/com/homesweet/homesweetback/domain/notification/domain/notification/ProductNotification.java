package com.homesweet.homesweetback.domain.notification.domain.notification;

import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;

import lombok.Builder;
import lombok.Getter;


/**
 * 상품 관련 알림 클래스
 * 
 * 상품 관련 알림들을 내부 클래스로 그룹화합니다.
 * 
 * @author dogyungkim
 */
public class ProductNotification {
    
    /**
     * 상품 승인 알림
     * 
     * 📋 필요한 필드:
     * - userName: String - 사용자 이름
     * - productId: Long - 상품 ID
     * - productName: String - 상품명
     */
    @Getter
    public static class ProductApproved implements TemplateNotification {
        private final String userName;
        private final Long productId;
        private final String productName;
        
        private final NotificationTemplateType eventType = NotificationTemplateType.PRODUCT_APPROVED;
        
        @Builder
        public ProductApproved(String userName, Long productId, String productName) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for PRODUCT_APPROVED notification");
            }
            if (productId == null) {
                throw new IllegalArgumentException("productId is required for PRODUCT_APPROVED notification");
            }
            if (productName == null || productName.isBlank()) {
                throw new IllegalArgumentException("productName is required for PRODUCT_APPROVED notification");
            }
            this.userName = userName;
            this.productId = productId;
            this.productName = productName;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
    }
    
    /**
     * 상품 거부 알림
     * 
     * 📋 필요한 필드:
     * - userName: String - 사용자 이름
     * - productId: Long - 상품 ID
     * - productName: String - 상품명
     */
    @Getter
    public static class ProductRejected implements TemplateNotification {
        private final String userName;
        private final Long productId;
        private final String productName;
        
        private final NotificationTemplateType eventType = NotificationTemplateType.PRODUCT_REJECTED;
        
        @Builder
        public ProductRejected(String userName, Long productId, String productName) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for PRODUCT_REJECTED notification");
            }
            if (productId == null) {
                throw new IllegalArgumentException("productId is required for PRODUCT_REJECTED notification");
            }
            if (productName == null || productName.isBlank()) {
                throw new IllegalArgumentException("productName is required for PRODUCT_REJECTED notification");
            }
            this.userName = userName;
            this.productId = productId;
            this.productName = productName;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
    }
    
    /**
     * 재고 부족 알림
     * 
     * 📋 필요한 필드:
     * - productId: Long - 상품 ID
     * - productName: String - 상품명
     * - currentStock: String - 현재 재고 수량
     */
    @Getter
    public static class ProductLowStock implements TemplateNotification {
        private final Long productId;
        private final String productName;
        private final String currentStock;
        
        private final NotificationTemplateType eventType = NotificationTemplateType.PRODUCT_LOW_STOCK;
        
        @Builder
        public ProductLowStock(Long productId, String productName, String currentStock) {
            if (productId == null) {
                throw new IllegalArgumentException("productId is required for PRODUCT_LOW_STOCK notification");
            }
            if (productName == null || productName.isBlank()) {
                throw new IllegalArgumentException("productName is required for PRODUCT_LOW_STOCK notification");
            }
            if (currentStock == null || currentStock.isBlank()) {
                throw new IllegalArgumentException("currentStock is required for PRODUCT_LOW_STOCK notification");
            }
            this.productId = productId;
            this.productName = productName;
            this.currentStock = currentStock;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
    }
    
    /**
     * 새 리뷰 등록 알림
     * 
     * 📋 필요한 필드:
     * - userName: String - 사용자 이름
     * - productId: Long - 상품 ID
     * - productName: String - 상품명
     */
    @Getter
    public static class NewReview implements TemplateNotification {
        private final String userName;
        private final Long productId;
        private final String productName;
        
        private final NotificationTemplateType eventType = NotificationTemplateType.NEW_REVIEW;
        
        @Builder
        public NewReview(String userName, Long productId, String productName) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for NEW_REVIEW notification");
            }
            if (productId == null) {
                throw new IllegalArgumentException("productId is required for NEW_REVIEW notification");
            }
            if (productName == null || productName.isBlank()) {
                throw new IllegalArgumentException("productName is required for NEW_REVIEW notification");
            }
            this.userName = userName;
            this.productId = productId;
            this.productName = productName;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
    }
}
