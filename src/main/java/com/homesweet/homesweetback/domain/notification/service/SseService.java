package com.homesweet.homesweetback.domain.notification.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseService {
    SseEmitter subscribe(Long userId);
    void sendNotification(Long userId, String data);
}
