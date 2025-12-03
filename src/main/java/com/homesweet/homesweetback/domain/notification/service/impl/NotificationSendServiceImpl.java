package com.homesweet.homesweetback.domain.notification.service.impl;

import com.homesweet.homesweetback.domain.notification.domain.event.CustomNotificationEvent;
import com.homesweet.homesweetback.domain.notification.domain.event.TemplateNotificationEvent;
import com.homesweet.homesweetback.domain.notification.domain.notification.CustomNotification;
import com.homesweet.homesweetback.domain.notification.domain.notification.TemplateNotification;
import com.homesweet.homesweetback.domain.notification.service.NotificationSendService;

import io.opentelemetry.instrumentation.annotations.WithSpan;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 알림 전송 서비스 구현
 * 
 * 이벤트 기반 아키텍처로 전환되어 실제 알림 처리는 NotificationEventListener에서 비동기로 처리됩니다.
 * 
 * @author dogyungkim
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSendServiceImpl implements NotificationSendService {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 템플릿 알림 전송 메서드 (단일 사용자)
     * 
     * 이벤트를 발행하여 비동기로 알림을 처리합니다.
     */
    @Override
    @WithSpan
    public void sendTemplateNotificationToSingleUser(Long userId, TemplateNotification notification) {
        log.debug("템플릿 알림 이벤트 발행: userId={}, eventType={}", userId, notification.getEventType());
        eventPublisher.publishEvent(new TemplateNotificationEvent(userId, notification));
    }

    /**
     * 템플릿 알림 전송 메서드 (다수 사용자)
     * 
     * 이벤트를 발행하여 비동기로 알림을 처리합니다.
     */
    @Override
    @WithSpan
    public void sendTemplateNotificationToMultipleUsers(List<Long> userIds, TemplateNotification notification) {
        log.debug("다수 사용자 템플릿 알림 이벤트 발행: userIds={}", userIds);
        eventPublisher.publishEvent(new TemplateNotificationEvent(userIds, notification));
    }

    /**
     * 커스텀 알림 전송 메서드 (단일 사용자)
     * 
     * 이벤트를 발행하여 비동기로 알림을 처리합니다.
     */
    @Override
    @WithSpan
    public void sendCustomNotificationToSingleUser(Long userId, CustomNotification notification) {
        log.debug("커스텀 알림 이벤트 발행: userId={}, title={}", userId, notification.getTitle());
        CustomNotificationEvent event = new CustomNotificationEvent(userId, notification);
        eventPublisher.publishEvent(event);
    }

    /**
     * 커스텀 알림 전송 메서드 (다수 사용자)
     * 
     * 이벤트를 발행하여 비동기로 알림을 처리합니다.
     */
    @Override
    @WithSpan
    public void sendCustomNotificationToMultipleUsers(List<Long> userIds, CustomNotification notification) {
        log.debug("다수 사용자 커스텀 알림 이벤트 발행: userIds={}, title={}", userIds, notification.getTitle());
        CustomNotificationEvent event = new CustomNotificationEvent(userIds, notification);
        eventPublisher.publishEvent(event);
    }
}