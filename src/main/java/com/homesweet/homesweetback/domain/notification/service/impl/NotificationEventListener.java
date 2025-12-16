package com.homesweet.homesweetback.domain.notification.service.impl;

import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;
import com.homesweet.homesweetback.domain.notification.domain.event.CustomNotificationEvent;
import com.homesweet.homesweetback.domain.notification.domain.event.TemplateNotificationEvent;
import com.homesweet.homesweetback.domain.notification.domain.notification.CustomNotification;
import com.homesweet.homesweetback.domain.notification.domain.notification.TemplateNotification;
import com.homesweet.homesweetback.domain.notification.dto.PushNotificationDTO;
import com.homesweet.homesweetback.domain.notification.entity.NotificationTemplate;
import com.homesweet.homesweetback.domain.notification.entity.UserNotification;
import com.homesweet.homesweetback.domain.notification.service.SseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.annotations.DynamicUpdate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 알림 이벤트 리스너
 * 
 * 이벤트 기반으로 알림을 비동기 처리합니다.
 * 
 * @author dogyungkim
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

        private final UserNotificationService userNotificationService;
        private final SseService sseService;

        /**
         * 템플릿 알림 이벤트 처리
         * 
         * 단일 사용자 또는 다수 사용자 모두 처리합니다.
         * TemplateNotification을 통해 DB에서 템플릿을 조회하고, Payload와 함께 알림을 전송합니다.
         */
        @Async("notificationTaskExecutor")
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
        public void handleTemplateNotificationEvent(TemplateNotificationEvent event) {
                log.info("템플릿 알림 이벤트 처리 시작: userIds={}, eventType={}", event.userIds(),
                                event.notification().getEventType());

                TemplateNotification notification = event.notification();

                // 1. 템플릿 조회 (DB에서 조회)
                NotificationTemplate template = userNotificationService
                                .getNotificationTemplate(notification.getEventType());

                log.info("템플릿 조회 완료: template={}", template);

                // 2. 각 사용자에게 알림 전송
                // 개별 사용자 실패는 전체 처리를 중단하지 않음
                for (Long userId : event.userIds()) {
                        try {
                                // 3. 알림 저장
                                UserNotification userNotification = userNotificationService
                                                .createAndSaveUserNotification(userId, template, notification.toMap());

                                // 4. 알림 DTO 생성
                                PushNotificationDTO pushNotificationDTO = buildPushNotificationDTO(notification.toMap(),
                                                template, userNotification.getId());

                                // 5. 푸시 전송
                                sseService.sendNotification(userId, pushNotificationDTO.toJson());

                        } catch (Exception e) {
                                log.error("사용자별 알림 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
                                // 개별 사용자 실패는 전체 처리를 중단하지 않음
                        }
                }
        }

        /**
         * 커스텀 알림 이벤트 처리
         */
        @Async("notificationTaskExecutor")
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
        public void handleCustomNotificationEvent(CustomNotificationEvent event) {
                log.info("커스텀 알림 이벤트 처리 시작: userIds={}, categoryType={}, title={}", event.userIds(),
                                event.notification().getTitle(), event.notification().getContent());
                CustomNotification notification = event.notification();
                // 1. 커스텀 알림 템플릿 생성
                // 템플릿 생성 실패 시 전체 이벤트 처리를 중단해야 함
                NotificationTemplate template = userNotificationService.createAndSaveCustomNotificationTemplate(
                                notification.getTitle(),
                                notification.getContent(),
                                notification.getRedirectUrl());

                // 2. 각 사용자에게 알림 전송
                // 개별 사용자 실패는 전체 처리를 중단하지 않음
                for (Long userId : event.userIds()) {
                        try {
                                // 3. 알림 저장
                                UserNotification userNotification = userNotificationService
                                                .createAndSaveUserNotification(userId, template,
                                                                event.notification().toMap());

                                // 4. 알림 DTO 생성
                                PushNotificationDTO pushNotificationDTO = buildPushNotificationDTO(
                                                event.notification().toMap(),
                                                template,
                                                userNotification.getId());

                                // 5. 푸시 전송
                                log.info("커스텀 알림 전송 완료: userId={}, notificationId={}", userId,
                                                userNotification.getId());
                                sseService.sendNotification(userId, pushNotificationDTO.toJson());

                        } catch (Exception e) {
                                log.error("사용자별 커스텀 알림 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
                                // 개별 사용자 실패는 전체 처리를 중단하지 않음
                        }
                }
        }

        // 내부 메서드

        private PushNotificationDTO buildPushNotificationDTO(
                        Map<String, Object> contextData,
                        NotificationTemplate template,
                        Long notificationId) {
                return PushNotificationDTO.builder()
                                .notificationId(notificationId)
                                .title(template.getTitle())
                                .content(template.getContent())
                                .redirectUrl(template.getRedirectUrl())
                                .contextData(contextData)
                                .categoryType(NotificationCategoryType.fromCategoryId(template.getCategory().getId()))
                                .isRead(false)
                                .createdAt(LocalDateTime.now())
                                .build();
        }
}
