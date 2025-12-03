package com.homesweet.homesweetback.domain.notification.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.homesweet.homesweetback.domain.notification.dto.PushNotificationDTO;

import java.util.Map;

public interface SseService {
    SseEmitter subscribe(Long userId);

    void sendNotification(Long userId, PushNotificationDTO data);

    /**
     * 다수 사용자에게 SSE 알림을 병렬로 전송
     * 
     * @param notifications 사용자 ID와 알림 데이터의 맵 (userId -> contextData)
     */
    void sendNotifications(Map<Long, PushNotificationDTO> notifications);
}
