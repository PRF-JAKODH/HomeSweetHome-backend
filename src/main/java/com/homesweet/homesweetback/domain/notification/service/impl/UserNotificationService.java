package com.homesweet.homesweetback.domain.notification.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;
import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;
import com.homesweet.homesweetback.domain.notification.entity.NotificationCategory;
import com.homesweet.homesweetback.domain.notification.entity.NotificationTemplate;
import com.homesweet.homesweetback.domain.notification.entity.UserNotification;
import com.homesweet.homesweetback.domain.notification.exception.NotificationException;
import com.homesweet.homesweetback.domain.notification.repository.NotificationCategoryRepository;
import com.homesweet.homesweetback.domain.notification.repository.NotificationTemplateRepository;
import com.homesweet.homesweetback.domain.notification.repository.UserNotificationJdbcRepository;

import io.opentelemetry.instrumentation.annotations.WithSpan;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * 사용자 알림을 생성하고 저장하는 서비스입니다.
 * 
 * @author dogyungkim
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserNotificationService {

    private final NotificationCategoryRepository notificationCategoryRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final UserNotificationJdbcRepository userNotificationJdbcRepository;

    /**
     * 사용자 알림을 대량으로 저장합니다.
     * 
     * @param userNotifications 사용자 알림 리스트
     * @throws IllegalArgumentException 알림 리스트가 null인 경우
     */
    @Transactional
    @WithSpan
    public void bulkInsertUserNotifications(List<UserNotification> userNotifications) {
        if (userNotifications == null) {
            throw new IllegalArgumentException("사용자 알림 리스트는 null일 수 없습니다.");
        }

        if (userNotifications.isEmpty()) {
            log.warn("저장할 사용자 알림이 없습니다.");
            return;
        }

        userNotificationJdbcRepository.saveAll(userNotifications);
    }

    /**
     * 사용자 알림을 생성합니다.
     * 
     * @param userId      사용자 ID
     * @param template    알림 템플릿
     * @param contextData 알림 컨텍스트 데이터
     * @return 생성된 사용자 알림
     * @throws IllegalArgumentException userId나 template이 null인 경우
     */
    @WithSpan
    public UserNotification createUserNotification(
            User user,
            NotificationTemplate template,
            Map<String, Object> contextData) {
        if (user == null) {
            throw new IllegalArgumentException("사용자 ID는 null일 수 없습니다.");
        }
        if (template == null) {
            throw new IllegalArgumentException("알림 템플릿은 null일 수 없습니다.");
        }

        return UserNotification.builder()
                .user(user)
                .template(template)
                .contextData(contextData != null ? contextData : Map.of())
                .isRead(false)
                .isDeleted(false)
                .build();
    }

    /**
     * 커스텀 알림을 템플릿을 생성합니다.
     * 
     * @param title       알림 제목
     * @param content     알림 내용
     * @param redirectUrl 알림 리다이렉트 URL
     * @return 생성된 커스텀 알림 템플릿
     * @throws IllegalArgumentException title이나 content가 null이거나 빈 문자열인 경우
     */
    @Transactional
    @WithSpan
    public NotificationTemplate createAndSaveCustomNotificationTemplate(
            String title,
            String content,
            String redirectUrl) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("알림 제목은 비어있을 수 없습니다.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("알림 내용은 비어있을 수 없습니다.");
        }

        NotificationCategory category = notificationCategoryRepository
                .getReferenceById(NotificationCategoryType.CUSTOM.getCategoryId());

        NotificationTemplate template = NotificationTemplate.builder()
                .category(category)
                .templateType(NotificationTemplateType.CUSTOM)
                .title(title)
                .content(content)
                .redirectUrl(redirectUrl != null ? redirectUrl : "")
                .build();
        return notificationTemplateRepository.save(template);
    }

    /**
     * 알림 템플릿 조회
     * 
     * @param eventType 알림 템플릿 타입
     * @return 조회된 알림 템플릿
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "notificationTemplateCache", key = "#eventType", cacheManager = "localCacheManager")
    @WithSpan
    public NotificationTemplate getNotificationTemplate(NotificationTemplateType eventType) {
        return notificationTemplateRepository
                .findByTemplateType(eventType)
                .orElseThrow(() -> new NotificationException(
                        ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND,
                        "알림 템플릿을 찾을 수 없습니다. eventType: " + eventType));
    }
}
