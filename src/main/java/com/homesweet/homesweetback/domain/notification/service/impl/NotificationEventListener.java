package com.homesweet.homesweetback.domain.notification.service.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;
import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;
import com.homesweet.homesweetback.domain.notification.domain.event.CustomNotificationEvent;
import com.homesweet.homesweetback.domain.notification.domain.event.TemplateNotificationEvent;
import com.homesweet.homesweetback.domain.notification.domain.notification.CustomNotification;
import com.homesweet.homesweetback.domain.notification.domain.notification.TemplateNotification;
import com.homesweet.homesweetback.domain.notification.dto.PushNotificationDTO;
import com.homesweet.homesweetback.domain.notification.entity.NotificationCategory;
import com.homesweet.homesweetback.domain.notification.entity.NotificationTemplate;
import com.homesweet.homesweetback.domain.notification.entity.UserNotification;
import com.homesweet.homesweetback.domain.notification.exception.NotificationException;
import com.homesweet.homesweetback.domain.notification.repository.NotificationCategoryRepository;
import com.homesweet.homesweetback.domain.notification.repository.NotificationTemplateRepository;
import com.homesweet.homesweetback.domain.notification.repository.UserNotificationRepository;
import com.homesweet.homesweetback.domain.notification.service.SseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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

    private final NotificationTemplateRepository notificationTemplateRepository;
    private final NotificationCategoryRepository notificationCategoryRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;
    private final SseService sseService;

    /**
     * 템플릿 알림 이벤트 처리
     * 
     * 단일 사용자 또는 다수 사용자 모두 처리합니다.
     * TemplateNotification을 통해 DB에서 템플릿을 조회하고, Payload와 함께 알림을 전송합니다.
     */
    @Async("notificationTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleTemplateNotificationEvent(TemplateNotificationEvent event) {
        log.info("템플릿 알림 이벤트 처리 시작: userIds={}, eventType={}", event.userIds(), event.notification().getEventType());

        TemplateNotification notification = event.notification();

        // 1. 템플릿 조회 (DB에서 조회)
        // 템플릿이 없으면 전체 이벤트 처리를 중단해야 함
        NotificationTemplate template = getNotificationTemplate(notification.getEventType());

        log.info("템플릿 조회 완료: template={}", template);

        // 2. 각 사용자에게 알림 전송
        // 개별 사용자 실패는 전체 처리를 중단하지 않음
        for (Long userId : event.userIds()) {
            try {
                // 3. User 조회
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new NotificationException(ErrorCode.USER_NOT_FOUND,
                                "사용자를 찾을 수 없습니다. userId: " + userId));

                // 4. 알림 저장
                UserNotification userNotification = createAndSaveUserNotification(user, template, notification.toMap());

                // 5. 템플릿 렌더링 (DTO 생성)
                PushNotificationDTO pushNotificationDTO = buildPushNotificationDTO(notification.toMap(), template,
                        userNotification.getId());

                // 6. 푸시 전송
                log.info("알림 전송 완료: userId={}, eventType={}, notificationId={}", userId, notification.getEventType(),
                        userNotification.getId());
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
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCustomNotificationEvent(CustomNotificationEvent event) {
        log.info("커스텀 알림 이벤트 처리 시작: userIds={}, categoryType={}, title={}", event.userIds(),
                event.notification().getTitle(), event.notification().getContent());
        CustomNotification notification = event.notification();
        // 1. 커스텀 알림 템플릿 생성
        // 템플릿 생성 실패 시 전체 이벤트 처리를 중단해야 함
        NotificationTemplate template = createAndSaveCustomNotificationTemplate(
                notification.getTitle(),
                notification.getContent(),
                notification.getRedirectUrl());

        // 2. 각 사용자에게 알림 전송
        // 개별 사용자 실패는 전체 처리를 중단하지 않음
        for (Long userId : event.userIds()) {
            try {
                // 3. User 조회
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new NotificationException(ErrorCode.USER_NOT_FOUND,
                                "사용자를 찾을 수 없습니다. userId: " + userId));

                // 4. 알림 저장
                UserNotification userNotification = createAndSaveUserNotification(user, template,
                        event.notification().toMap());

                // 5. 푸시 전송
                PushNotificationDTO pushNotificationDTO = buildPushNotificationDTO(
                        event.notification().toMap(),
                        template,
                        userNotification.getId());

                log.info("커스텀 알림 전송 완료: userId={}, notificationId={}", userId, userNotification.getId());
                sseService.sendNotification(userId, pushNotificationDTO.toJson());

            } catch (Exception e) {
                log.error("사용자별 커스텀 알림 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
                // 개별 사용자 실패는 전체 처리를 중단하지 않음
            }
        }
    }

    // Private 메서드들

    private NotificationTemplate getNotificationTemplate(NotificationTemplateType eventType) {
        return notificationTemplateRepository
                .findByTemplateType(eventType)
                .orElseThrow(() -> new NotificationException(
                        ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND,
                        "알림 템플릿을 찾을 수 없습니다. eventType: " + eventType));
    }

    private UserNotification createAndSaveUserNotification(User user, NotificationTemplate template,
            Map<String, Object> contextData) {
        UserNotification userNotification = UserNotification.builder()
                .user(user)
                .template(template)
                .contextData(contextData)
                .isRead(false)
                .isDeleted(false)
                .build();
        return userNotificationRepository.save(userNotification);
    }

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

    private NotificationTemplate createAndSaveCustomNotificationTemplate(
            String title,
            String content,
            String redirectUrl) {
        NotificationCategory category = notificationCategoryRepository
                .getReferenceById(NotificationCategoryType.CUSTOM.getCategoryId());

        NotificationTemplate template = NotificationTemplate.builder()
                .category(category)
                .templateType(NotificationTemplateType.CUSTOM)
                .title(title)
                .content(content)
                .redirectUrl(redirectUrl)
                .build();
        return notificationTemplateRepository.save(template);
    }
}
