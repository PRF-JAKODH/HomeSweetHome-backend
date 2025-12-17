package com.homesweet.homesweetback.domain.notification.service.impl;

import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.service.UserService;
import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;
import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;
import com.homesweet.homesweetback.domain.notification.entity.NotificationCategory;
import com.homesweet.homesweetback.domain.notification.entity.NotificationTemplate;
import com.homesweet.homesweetback.domain.notification.entity.UserNotification;
import com.homesweet.homesweetback.domain.notification.exception.NotificationException;
import com.homesweet.homesweetback.domain.notification.repository.NotificationCategoryRepository;
import com.homesweet.homesweetback.domain.notification.repository.NotificationTemplateRepository;
import com.homesweet.homesweetback.domain.notification.repository.UserNotificationRepository;

import jakarta.persistence.EntityManager;
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

        private final UserService userService;
        private final NotificationCategoryRepository notificationCategoryRepository;
        private final NotificationTemplateRepository notificationTemplateRepository;
        private final UserNotificationRepository userNotificationRepository;

        /**
         * 사용자 알림 생성 및 저장
         * 
         * @param userId      사용자 ID
         * @param template    알림 템플릿
         * @param contextData 알림 컨텍스트 데이터
         * @return 생성된 사용자 알림
         */
        @Transactional
        public UserNotification createAndSaveUserNotification(
                        Long userId,
                        NotificationTemplate template,
                        Map<String, Object> contextData) {
                UserNotification userNotification = UserNotification.builder()
                                .user(userService.getUserById(userId))
                                .template(template)
                                .contextData(contextData)
                                .isRead(false)
                                .isDeleted(false)
                                .build();
                return userNotificationRepository.save(userNotification);
        }

        /**
         * 커스텀 알림 템플릿 생성 및 저장
         * 
         * @param title       알림 제목
         * @param content     알림 내용
         * @param redirectUrl 알림 리다이렉트 URL
         * @return 생성된 커스텀 알림 템플릿
         */
        @Transactional
        public NotificationTemplate createAndSaveCustomNotificationTemplate(
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

        /**
         * 알림 템플릿 조회
         * 
         * @param eventType 알림 템플릿 타입
         * @return 조회된 알림 템플릿
         */
        @Transactional(readOnly = true)
        @Cacheable(value = "notificationTemplateCache", key = "#eventType", cacheManager = "localCacheManager")
        public NotificationTemplate getNotificationTemplate(NotificationTemplateType eventType) {
                return notificationTemplateRepository
                                .findByTemplateType(eventType)
                                .orElseThrow(() -> new NotificationException(
                                                ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND,
                                                "알림 템플릿을 찾을 수 없습니다. eventType: " + eventType));
        }
}
