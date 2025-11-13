package com.homesweet.homesweetback.domain.notification.domain.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;

import java.util.Map;

/**
 * 템플릿 기반 알림 인터페이스
 * 
 * EventType과 데이터를 하나로 묶어 타입 안전성을 제공합니다.
 * 
 * @author dogyungkim
 */
public interface TemplateNotification {
    
    /**
     * 알림 이벤트 타입 반환
     * 
     * @return NotificationTemplateType
     */
    NotificationTemplateType getEventType();
    
    /**
     * 알림 데이터를 Map으로 변환
     * ObjectMapper를 사용하여 자동으로 변환합니다.
     * eventType 필드는 각 Notification 클래스에서 @JsonIgnore로 변환 전에 제외됩니다.
     * 
     * @return 변환된 Map 객체 (eventType 제외)
     */
    @SuppressWarnings("unchecked")
    default Map<String, Object> toMap() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(this, Map.class);
    }
    
    /**
     * 알림 검증
     * 필수 필드 검증을 수행합니다.
     * 
     * @throws IllegalArgumentException 필수 필드 누락 시
     */
    void validate();
}

