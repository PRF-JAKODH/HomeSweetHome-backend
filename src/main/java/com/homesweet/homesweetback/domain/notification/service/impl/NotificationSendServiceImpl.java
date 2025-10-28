package com.homesweet.homesweetback.domain.notification.service.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;
import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;
import com.homesweet.homesweetback.domain.notification.domain.payload.NotificationPayload;
import com.homesweet.homesweetback.domain.notification.dto.PushNotificationDTO;
import com.homesweet.homesweetback.domain.notification.entity.NotificationTemplate;
import com.homesweet.homesweetback.domain.notification.entity.UserNotification;
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
    
    @Override
    public void sendTemplateNotificationToSingleUser(Long userId, NotificationTemplateType templateType) {
        log.info("알림 전송: userId={}, templateType={}", userId, templateType);
    }
    
    @Override
    public void sendTemplateNotificationToSingleUser(Long userId, NotificationTemplateType templateType, NotificationPayload payload) {
        // 1. Payload 검증
        payload.validate();
        // 2. 템플릿 조회
        NotificationTemplate template = getNotificationTemplate(templateType);

        // 3. 템플릿 렌더링 (DTO 생성)
        PushNotificationDTO pushNotificationDTO = buildPushNotificationDTO(payload, template);

        // 4. 알림 저장
        createAndSaveUserNotification(userId, template, pushNotificationDTO.getContextData());

        // 5. 푸시 전송
        log.info("알림 전송: userId={}, templateType={}, contextData={}", userId, templateType, pushNotificationDTO.toJson());
        sseService.sendNotification(userId, pushNotificationDTO.toJson());
    }

    @Override   
    public void sendTemplateNotificationToMultipleUsers(List<Long> userIds, NotificationTemplateType templateType) {
        log.info("알림 전송: userIds={}, templateType={}", userIds, templateType);
        // TODO: 실제 알림 전송 구현
    }
    
    @Override
    public void sendTemplateNotificationToMultipleUsers(List<Long> userIds, NotificationTemplateType templateType, NotificationPayload payload) {
        // 1. Payload 검증
        payload.validate();
        // 2. 템플릿 조회
        NotificationTemplate template = getNotificationTemplate(templateType);

        // 3. 템플릿 렌더링 (DTO 생성)
        PushNotificationDTO pushNotificationDTO = buildPushNotificationDTO(payload, template);

        for (Long userId : userIds) {
            // 4. 알림 저장
            createAndSaveUserNotification(userId, template, pushNotificationDTO.getContextData());

            // 5. 푸시 전송
            log.info("알림 전송: userId={}, templateType={}, contextData={}", userId, templateType, pushNotificationDTO.toJson());
            sseService.sendNotification(userId, pushNotificationDTO.toJson());
        }
    }

    @Override
    public void sendCustomNotificationToSingleUser(Long userId, NotificationCategoryType categoryType, String title, String content, String redirectUrl, Map<String, Object> contextData) {
        log.info("커스텀 알림 전송: userId={}, categoryType={}, title={}", userId, categoryType, title);
        // TODO: 실제 알림 전송 구현
    }

    @Override
    public void sendCustomNotificationToMultipleUsers(List<Long> userIds, NotificationCategoryType categoryType, String title, String content, String redirectUrl, Map<String, Object> contextData) {
        log.info("커스텀 알림 전송: userIds={}, categoryType={}, title={}", userIds, categoryType, title);
        // TODO: 실제 알림 전송 구현
    }
    

    private NotificationTemplate getNotificationTemplate(NotificationTemplateType templateType) {
        return notificationTemplateRepository.findByTemplateType(templateType).orElseThrow(() -> new RuntimeException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND.getMessage()));
    }

    @Transactional
    private void createAndSaveUserNotification(Long userId, NotificationTemplate template, Map<String, Object> contextData) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        UserNotification userNotification = UserNotification.builder()
            .user(user)
            .template(template)
            .contextData(contextData)
            .isRead(false)
            .isDeleted(false)
            .build();   
        userNotificationRepository.save(userNotification);
    }

    private PushNotificationDTO buildPushNotificationDTO(NotificationPayload payload, NotificationTemplate template) {
        PushNotificationDTO pushNotificationDTO = PushNotificationDTO.builder()
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
}