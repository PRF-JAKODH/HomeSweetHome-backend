package com.homesweet.homesweetback.domain.notification.service.impl;

import com.homesweet.homesweetback.domain.notification.dto.PushNotificationDTO;
import com.homesweet.homesweetback.domain.notification.service.SseService;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class InMemorySseService implements SseService {
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30분
    private final Map<Long, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    private final Executor sseTaskExecutor;

    public InMemorySseService(@Qualifier("sseTaskExecutor") Executor sseTaskExecutor) {
        this.sseTaskExecutor = sseTaskExecutor;
    }

    /**
     * SSE 연결 생성
     * 
     * @param userId 사용자 ID
     * @return SseEmitter
     */
    @WithSpan
    @Override
    public SseEmitter subscribe(Long userId) {

        // 기존 연결이 있으면 정리
        SseEmitter existingEmitter = sseEmitters.get(userId);

        if (existingEmitter != null) {
            try {
                existingEmitter.complete();
            } catch (Exception e) {
                log.error("기존 SSE 연결 정리 실패: userId={}", userId);
            }
        }

        // 새 연결 생성
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        sseEmitters.put(userId, emitter);

        emitter.onCompletion(() -> removeEmitter(userId));

        emitter.onTimeout(() -> {
            log.debug("SSE 연결 타임아웃: userId={}", userId);
            removeEmitter(userId);
        });

        emitter.onError((e) -> {
            log.warn("SSE 연결 에러: userId={}, error={}", userId, e.getMessage());
            removeEmitter(userId);
        });

        // 연결 확인
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (Exception e) {
            emitter.completeWithError(e);
            log.error("SSE 연결 실패: userId={}", userId);
        }

        return emitter;
    }

    /**
     * SSE 알림 전송 (비동기 처리)
     * 
     * 단일 사용자에게 알림을 전송합니다.
     * 
     * @param userId      사용자 ID
     * @param contextData 전송할 알림 데이터 (JSON 문자열)
     */
    @WithSpan
    @Override
    @Async("sseTaskExecutor")
    public void sendNotification(Long userId, PushNotificationDTO contextData) {
        sendNotificationInternal(userId, contextData);
    }

    /**
     * 다수 사용자에게 SSE 알림을 병렬로 전송 (비동기 처리, 논블로킹)
     * 
     * @param notifications 사용자 ID와 알림 데이터의 맵 (userId -> contextData)
     */
    @WithSpan
    @Override
    public void sendNotifications(Map<Long, PushNotificationDTO> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            log.warn("전송할 알림이 없습니다.");
            return;
        }

        log.info("다수 사용자 SSE 알림 전송 시작 (비동기): count={}", notifications.size());

        notifications.forEach((userId, contextData) -> {
            sseTaskExecutor.execute(() -> sendNotificationInternal(userId, contextData));
        });
    }

    /**
     * SSE 알림 전송 내부 구현 (동기 처리)
     * 
     * 실제 SSE 전송 로직을 수행합니다.
     * sendNotifications 내부에서 직접 호출할 때 사용됩니다.
     * 
     * @param userId      사용자 ID
     * @param contextData 전송할 알림 데이터 (JSON 문자열)
     */
    @WithSpan
    private void sendNotificationInternal(Long userId, PushNotificationDTO contextData) {

        SseEmitter emitter = sseEmitters.get(userId);
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event().name("notification").data(contextData, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.warn("SSE 알림 전송 실패 (클라이언트 연결 끊김): userId={}", userId);
            removeEmitter(userId);
        } catch (Exception e) {
            log.error("SSE 알림 전송 중 예외: userId={}", userId, e);
            removeEmitter(userId);
            emitter.completeWithError(e);
        }
    }

    @WithSpan
    private void removeEmitter(Long userId) {
        SseEmitter emitter = sseEmitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
        }
    }
}
