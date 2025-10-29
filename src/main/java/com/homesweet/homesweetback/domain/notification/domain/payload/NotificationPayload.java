package com.homesweet.homesweetback.domain.notification.domain.payload;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;
import com.homesweet.homesweetback.domain.notification.exception.NotificationException;

import java.util.Arrays;
import java.util.Map;

/**
 * 어노테이션 기반 검증을 사용하는 NotificationPayload
 * 
 * @SupportsEventType 어노테이션을 사용하여 각 Payload가 지원하는 EventType을 명시합니다.
 * 
 * @author dogyungkim
 */
public abstract class NotificationPayload {
    
    /**
     * Payload를 Map으로 변환
     * 
     * @return 변환된 Map 객체
     */
    public abstract Map<String, Object> toMap();
    
    /**
     * 어노테이션 기반 EventType 검증
     * 
     * @param eventType 검증할 EventType
     * @throws NotificationException EventType과 Payload가 일치하지 않는 경우
     */
    public void validate(NotificationEventType eventType) {
        // 1. EventType 지원 여부 확인
        if (!supportsEventType(eventType)) {
            throw new NotificationException(ErrorCode.NOTIFICATION_EVENT_TYPE_MISMATCH);
        }
        
        // 2. 필수 필드 검증
        validateRequiredFields();
    }
    
    /**
     * 어노테이션을 통해 지원하는 EventType 확인
     * 
     * @param eventType 확인할 EventType
     * @return 지원 여부
     */
    private boolean supportsEventType(NotificationEventType eventType) {
        SupportsEventType annotation = this.getClass().getAnnotation(SupportsEventType.class);
        if (annotation == null) {
            return false;
        }
        return Arrays.asList(annotation.value()).contains(eventType);
    }
    
    /**
     * 필수 필드 검증
     * 하위 클래스에서 오버라이드하여 구체적인 검증 로직을 구현합니다.
     * 
     * @throws IllegalArgumentException 필수 필드가 누락된 경우
     */
    protected void validateRequiredFields() {
        // 기본 구현 - 하위 클래스에서 오버라이드
    }
}