package com.homesweet.homesweetback.domain.notification.domain.payload;

import java.util.Map;

/**
 * 모든 알림 Payload가 구현해야 하는 인터페이스
 * 
 * Payload 객체는 toMap() 메서드를 통해 Map<String, Object>로 변환됩니다.
 * 이를 통해 템플릿 렌더링에 사용됩니다.
 * 
 * @author dogyungkim
 */
public interface NotificationPayload {
    
    /**
     * Payload를 Map으로 변환
     * 
     * @return 변환된 Map 객체
     */
    Map<String, Object> toMap();
    
    /**
     * Payload 검증
     * 필수 필드가 누락되었는지 확인
     * 
     * @throws IllegalArgumentException 필수 필드가 누락된 경우
     */
    default void validate() {
        // 기본 구현: 자식 클래스에서 오버라이드
        Map<String, Object> data = toMap();
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Payload data cannot be null or empty");
        }
    }
}

