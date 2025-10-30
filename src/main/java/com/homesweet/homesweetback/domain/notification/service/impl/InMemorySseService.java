package com.homesweet.homesweetback.domain.notification.service.impl;

import com.homesweet.homesweetback.domain.notification.service.SseService;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class InMemorySseService implements SseService{
    private final Map<Long,SseEmitter> sseEmitters = new HashMap<>();

    @Override
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(1000000L);
        sseEmitters.put(userId, emitter);

        emitter.onCompletion(() -> sseEmitters.remove(userId));
        emitter.onTimeout(() -> sseEmitters.remove(userId));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (Exception e) {
            sseEmitters.remove(userId);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @Override
    public void sendNotification(Long userId, String contextData) {
        SseEmitter emitter = sseEmitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(contextData));
            } catch (Exception e) {
                sseEmitters.remove(userId);
                emitter.completeWithError(e);
            }
        }
    }
}
