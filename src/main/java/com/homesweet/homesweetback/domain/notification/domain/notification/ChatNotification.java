package com.homesweet.homesweetback.domain.notification.domain.notification;

import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;

import lombok.Builder;
import lombok.Getter;


/**
 * 채팅 관련 알림 클래스
 * 
 * 채팅 관련 알림들을 내부 클래스로 그룹화합니다.
 * 
 * @author dogyungkim
 */
public class ChatNotification {
    
    /**
     * 새 메시지 알림
     * 
     * 📋 필요한 필드:
     * - userName: String - 메시지 발신자 이름
     * - roomId: String - 채팅방 ID
     * - roomName: String - 채팅방 이름
     * - message: String - 메시지 내용
     */
    @Getter
    public static class NewMessage implements TemplateNotification {
        private final String userName;
        private final Long roomId;
        private final String roomName;
        private final String message;
        
        private final NotificationTemplateType eventType = NotificationTemplateType.NEW_MESSAGE;
        
        @Builder
        public NewMessage(String userName, Long roomId, String roomName, String message) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for NEW_MESSAGE notification");
            }
            if (roomId == null) {
                throw new IllegalArgumentException("roomId is required for NEW_MESSAGE notification");
            }
            if (roomName == null || roomName.isBlank()) {
                throw new IllegalArgumentException("roomName is required for NEW_MESSAGE notification");
            }
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("message is required for NEW_MESSAGE notification");
            }
            this.userName = userName;
            this.roomId = roomId;
            this.roomName = roomName;
            this.message = message;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
    }
}
