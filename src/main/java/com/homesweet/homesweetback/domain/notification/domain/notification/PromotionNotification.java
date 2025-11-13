package com.homesweet.homesweetback.domain.notification.domain.notification;

import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;

import lombok.Builder;
import lombok.Getter;


/**
 * 프로모션 관련 알림 클래스
 * 
 * 프로모션 관련 알림들을 내부 클래스로 그룹화합니다.
 * 
 * @author dogyungkim
 */
public class PromotionNotification {
    
    /**
     * 프로모션 시작 알림
     * 
     * 📋 필요한 필드:
     * - promotionName: String - 프로모션 이름
     */
    @Getter
    public static class PromotionStart implements TemplateNotification {
        private final String promotionName;
        
        private final NotificationTemplateType eventType = NotificationTemplateType.PROMOTION_START;
        
        @Builder
        public PromotionStart(String promotionName) {
            if (promotionName == null || promotionName.isBlank()) {
                throw new IllegalArgumentException("promotionName is required for PROMOTION_START notification");
            }
            this.promotionName = promotionName;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
    }
    
    /**
     * 프로모션 종료 알림
     * 
     * 📋 필요한 필드:
     * - promotionName: String - 프로모션 이름
     */
    @Getter
    public static class PromotionEnd implements TemplateNotification {
        private final String promotionName;
        
        private final NotificationTemplateType eventType = NotificationTemplateType.PROMOTION_END;
        
        @Builder
        public PromotionEnd(String promotionName) {
            if (promotionName == null || promotionName.isBlank()) {
                throw new IllegalArgumentException("promotionName is required for PROMOTION_END notification");
            }
            this.promotionName = promotionName;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
    }
}
