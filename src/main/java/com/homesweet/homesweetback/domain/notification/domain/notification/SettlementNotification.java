package com.homesweet.homesweetback.domain.notification.domain.notification;

import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;

import lombok.Builder;
import lombok.Getter;


/**
 * 정산 관련 알림 클래스
 * 
 * 정산 관련 알림들을 내부 클래스로 그룹화합니다.
 * 
 * @author dogyungkim
 */
public class SettlementNotification {
    
    /**
     * 정산 완료 알림
     * 
     * 📋 필요한 필드:
     * - userName: String - 사용자 이름
     * - settlementId: String - 정산 ID
     * - amount: String - 정산 금액
     * - settlementName: String - 정산 이름
     */
    @Getter
    public static class SettlementCompleted implements TemplateNotification {
        private final String userName;
        private final String settlementId;
        private final String amount;
        private final String settlementName;
        
        private final NotificationTemplateType eventType = NotificationTemplateType.SETTLEMENT_COMPLETED;
        
        @Builder
        public SettlementCompleted(String userName, String settlementId, String amount, String settlementName) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for SETTLEMENT_COMPLETED notification");
            }
            if (settlementId == null || settlementId.isBlank()) {
                throw new IllegalArgumentException("settlementId is required for SETTLEMENT_COMPLETED notification");
            }
            if (amount == null || amount.isBlank()) {
                throw new IllegalArgumentException("amount is required for SETTLEMENT_COMPLETED notification");
            }
            if (settlementName == null || settlementName.isBlank()) {
                throw new IllegalArgumentException("settlementName is required for SETTLEMENT_COMPLETED notification");
            }
            this.userName = userName;
            this.settlementId = settlementId;
            this.amount = amount;
            this.settlementName = settlementName;
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
     * 정산 실패 알림
     * 
     * 📋 필요한 필드:
     * - userName: String - 사용자 이름
     * - settlementId: String - 정산 ID
     */
    @Getter
    public static class SettlementFailed implements TemplateNotification {
        private final String userName;
        private final String settlementId;
        
        private final NotificationTemplateType eventType = NotificationTemplateType.SETTLEMENT_FAILED;
        
        @Builder
        public SettlementFailed(String userName, String settlementId) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for SETTLEMENT_FAILED notification");
            }
            if (settlementId == null || settlementId.isBlank()) {
                throw new IllegalArgumentException("settlementId is required for SETTLEMENT_FAILED notification");
            }
            this.userName = userName;
            this.settlementId = settlementId;
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
