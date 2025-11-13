package com.homesweet.homesweetback.domain.notification.domain.notification;

import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;

import lombok.Builder;
import lombok.Getter;


/**
 * 시스템 관련 알림 클래스
 * 
 * 시스템 관련 알림들을 내부 클래스로 그룹화합니다.
 * 
 * @author dogyungkim
 */
public class SystemNotification {
    
    /**
     * 시스템 점검 알림
     * 
     * 📋 필요한 필드:
     * - maintenanceTime: String - 점검 시간
     */
    @Getter
    public static class SystemMaintenance implements TemplateNotification {
        private final String maintenanceTime;
        
        private final NotificationTemplateType eventType = NotificationTemplateType.SYSTEM_MAINTENANCE;
        
        @Builder
        public SystemMaintenance(String maintenanceTime) {
            if (maintenanceTime == null || maintenanceTime.isBlank()) {
                throw new IllegalArgumentException("maintenanceTime is required for SYSTEM_MAINTENANCE notification");
            }
            this.maintenanceTime = maintenanceTime;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
    }
    
    /**
     * 시스템 업데이트 알림
     * 
     * 📋 필요한 필드:
     * - version: String - 업데이트 버전
     * - updateFeatures: String - 업데이트 기능 목록
     */
    @Getter
    public static class SystemUpdate implements TemplateNotification {
        private final String version;
        private final String updateFeatures;
        
        private final NotificationTemplateType eventType = NotificationTemplateType.SYSTEM_UPDATE;
        
        @Builder
        public SystemUpdate(String version, String updateFeatures) {
            if (version == null || version.isBlank()) {
                throw new IllegalArgumentException("version is required for SYSTEM_UPDATE notification");
            }
            if (updateFeatures == null || updateFeatures.isBlank()) {
                throw new IllegalArgumentException("updateFeatures is required for SYSTEM_UPDATE notification");
            }
            this.version = version;
            this.updateFeatures = updateFeatures;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
    }
    
    /**
     * 판매자 등록 완료 알림
     * 
     * 📋 필요한 필드:
     * - userName: String - 사용자 이름
     */
    @Getter
    public static class SellerRegistrationComplete implements TemplateNotification {
        private final String userName;
        
        private final NotificationTemplateType eventType = NotificationTemplateType.SELLER_REGISTRATION_COMPLETE;
        
        @Builder
        public SellerRegistrationComplete(String userName) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for SELLER_REGISTRATION_COMPLETE notification");
            }
            this.userName = userName;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
    }
}
