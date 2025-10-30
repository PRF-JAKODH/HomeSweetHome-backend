package com.homesweet.homesweetback.domain.notification.service.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;
import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;
import com.homesweet.homesweetback.domain.notification.domain.payload.CustomNotificationPayload;
import com.homesweet.homesweetback.domain.notification.domain.payload.NotificationPayload;
import com.homesweet.homesweetback.domain.notification.dto.PushNotificationDTO;
import com.homesweet.homesweetback.domain.notification.entity.NotificationCategory;
import com.homesweet.homesweetback.domain.notification.entity.NotificationTemplate;
import com.homesweet.homesweetback.domain.notification.entity.UserNotification;
import com.homesweet.homesweetback.domain.notification.exception.NotificationException;
import com.homesweet.homesweetback.domain.notification.repository.NotificationTemplateRepository;
import com.homesweet.homesweetback.domain.notification.repository.UserNotificationRepository;
import com.homesweet.homesweetback.domain.notification.service.NotificationSendService;
import com.homesweet.homesweetback.domain.notification.service.SseService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSendServiceImpl implements NotificationSendService {
    
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;
    private final SseService sseService;
    

    /**
     * 템플릿 알림 전송 메서드
     * 
     * 
     * **/
    @Override
    public void sendTemplateNotificationToSingleUser(Long userId, NotificationEventType eventType, NotificationPayload payload) {
        // 1. Payload 검증
        payload.validate(eventType);
        // 2. 템플릿 조회
        NotificationTemplate template = getNotificationEventType(eventType);

        // 3. 알림 저장
        UserNotification userNotification = createAndSaveUserNotification(userId, template, payload.toMap());

        // 4. 템플릿 렌더링 (DTO 생성)
        PushNotificationDTO pushNotificationDTO = buildPushNotificationDTO(payload, template, userNotification.getId());

        // 5. 푸시 전송
        log.info("알림 전송: userId={}, eventType={}, contextData={}", userId, eventType, pushNotificationDTO.toJson());
        sseService.sendNotification(userId, pushNotificationDTO.toJson());
    }

    @Override
    public void sendTemplateNotificationToMultipleUsers(List<Long> userIds, NotificationEventType eventType, NotificationPayload payload) {
        // 1. Payload 검증
        payload.validate(eventType);
        // 2. 템플릿 조회
        NotificationTemplate template = getNotificationEventType(eventType);

        for (Long userId : userIds) {
            // 3. 알림 저장
            UserNotification userNotification = createAndSaveUserNotification(userId, template, payload.toMap());

            // 4. 템플릿 렌더링 (DTO 생성)
            PushNotificationDTO pushNotificationDTO = buildPushNotificationDTO(payload, template, userNotification.getId());

            // 5. 푸시 전송
            log.info("알림 전송: userId={}, eventType={}, contextData={}", userId, eventType, pushNotificationDTO.toJson());
            sseService.sendNotification(userId, pushNotificationDTO.toJson());
        }
    }

    /**
     * 커스텀 알림 전송
     * 
     *
     * **/

    @Override
    public void sendCustomNotificationToSingleUser(Long userId, NotificationCategoryType categoryType, String title, String content, String redirectUrl, Map<String, Object> contextData) {
        log.info("커스텀 알림 전송: userId={}, categoryType={}, title={}", userId, categoryType, title);
        // 1. 커스텀 알림 템플릿 생성
        NotificationTemplate template = createAndSaveCustomNotificationTemplate(categoryType, title, content, redirectUrl);
        // 2. 알림 저장
        UserNotification userNotification = createAndSaveUserNotification(userId, template, contextData);
        // 3. 푸시 전송
        sseService.sendNotification(userId, buildPushNotificationDTO(new CustomNotificationPayload(contextData), template, userNotification.getId()).toJson());
    }

    @Override
    public void sendCustomNotificationToMultipleUsers(List<Long> userIds, NotificationCategoryType categoryType, String title, String content, String redirectUrl, Map<String, Object> contextData) {
        log.info("커스텀 알림 전송: userIds={}, categoryType={}, title={}", userIds, categoryType, title);
        // 1. 커스텀 알림 템플릿 생성
        NotificationTemplate template = createAndSaveCustomNotificationTemplate(categoryType, title, content, redirectUrl);

        for (Long userId : userIds) {
        // 2. 알림 저장
            UserNotification userNotification = createAndSaveUserNotification(userId, template, contextData);
            // 3. 푸시 전송
            sseService.sendNotification(userId, buildPushNotificationDTO(new CustomNotificationPayload(contextData), template, userNotification.getId()).toJson());
        }
    }
    
    // private 메서드
    private NotificationTemplate getNotificationEventType(NotificationEventType eventType) {
        // DB 스키마 상에서는 TemplateType을 사용하고 있지만, 실제 구현에서는 EventType을 사용하고 있습니다.
        return notificationTemplateRepository
                        .findByTemplateType(eventType)
                        .orElseThrow(() -> new NotificationException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND, "알림 템플릿을 찾을 수 없습니다. eventType: " + eventType));
    }

    @Transactional
    public UserNotification createAndSaveUserNotification(Long userId, NotificationTemplate template, Map<String, Object> contextData) {
        // getReferenceById를 사용하여 프록시 객체만 생성 (DB 조회 없음)
        User user = userRepository.getReferenceById(userId);
        
        UserNotification userNotification = UserNotification.builder()
            .user(user)
            .template(template)
            .contextData(contextData)
            .isRead(false)
            .isDeleted(false)
            .build();   
        return userNotificationRepository.save(userNotification);
    }

    private PushNotificationDTO buildPushNotificationDTO(NotificationPayload payload, NotificationTemplate template, Long notificationId) {
        PushNotificationDTO pushNotificationDTO = PushNotificationDTO.builder()
            .notificationId(notificationId)
            .title(template.getTitle())
            .content(template.getContent())
            .redirectUrl(template.getRedirectUrl())
            .contextData(payload.toMap())
            .categoryType(template.getCategory().getCategoryType())
            .isRead(false)
            .createdAt(LocalDateTime.now())
            .build();
        return pushNotificationDTO;
    }
    /**
     * 커스텀 알림 템플릿 생성
     * @param categoryType 알림 카테고리 타입
     * @param title 알림 제목
     * @param content 알림 내용
     * @param redirectUrl 알림 리다이렉트 URL
     * @return 커스텀 알림 템플릿
     */
    private NotificationTemplate createAndSaveCustomNotificationTemplate(NotificationCategoryType categoryType, String title, String content, String redirectUrl) {
        NotificationTemplate template = NotificationTemplate.builder()
            .category(NotificationCategory.builder()
                .categoryType(NotificationCategoryType.CUSTOM)
                .build())
            .templateType(NotificationEventType.CUSTOM)
            .title(title)
            .content(content)
            .redirectUrl(redirectUrl)
            .build();
        return notificationTemplateRepository.save(template);
    }
}