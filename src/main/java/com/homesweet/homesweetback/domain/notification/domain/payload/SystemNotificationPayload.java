package com.homesweet.homesweetback.domain.notification.domain.payload;

import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;

import lombok.Builder;

import java.util.Map;

public class SystemNotificationPayload {

    /**
     * 판매자 등록 완료 알림 Payload
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     */
    @SupportsEventType(NotificationEventType.SELLER_REGISTRATION_COMPLETE)
    @Builder
    public static class SellerRegistrationCompletePayload extends NotificationPayload {
        private String userName;
        
        @Override
        public Map<String, Object> toMap() {
            return Map.of("userName", userName);
        }
        
        @Override
        protected void validateRequiredFields() {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for SELLER_REGISTRATION_COMPLETE notification");
            }
        }
    }
}
