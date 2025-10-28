package com.homesweet.homesweetback.domain.notification.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.notification.dto.PushNotificationDTO;
import com.homesweet.homesweetback.domain.notification.entity.UserNotification;
import com.homesweet.homesweetback.domain.notification.exception.NotificationException;
import com.homesweet.homesweetback.domain.notification.repository.UserNotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationAPIService {
    private final UserNotificationRepository userNotificationRepository;

    /**
     * 사용자의 알림 목록 조회 (최대 20개)
     * @param userId 사용자 ID
     * @return 알림 목록 (최대 20개)
     */
    @Transactional(readOnly = true)
    public List<PushNotificationDTO> getAllNotifications(Long userId) {
        List<UserNotification> userNotifications = userNotificationRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

        // List<UserNotification>을 List<PushNotificationDTO>로 변환하고 최대 20개로 제한
        return userNotifications.stream()
            .limit(20)
            .map(userNotification -> {
                var template = userNotification.getTemplate();
                return PushNotificationDTO.builder()
                    .title(template.getTitle())
                    .content(template.getContent())
                    .redirectUrl(template.getRedirectUrl())
                    .contextData(userNotification.getContextData())
                    .isRead(userNotification.getIsRead())
                    .categoryType(template.getCategory().getCategoryType())
                    .createdAt(userNotification.getCreatedAt())
                    .build();
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 사용자의 알림 읽음 처리 (단일 및 여러 개 모두 처리)
     * @param userId 사용자 ID
     * @param notificationIds 알림 ID 리스트
     */
    @Transactional
    public void markAsRead(Long userId, List<Long> notificationIds) {
        List<UserNotification> userNotifications = userNotificationRepository.findByIdInAndUserIdAndNotDeleted(notificationIds, userId);
        
        if (userNotifications.isEmpty()) {
            throw new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        
        userNotifications.forEach(UserNotification::markAsRead);
        userNotificationRepository.saveAll(userNotifications);
    }

    /**
     * 알림 삭제 처리 (단일 및 여러 개 모두 처리)
     * @param userId 사용자 ID
     * @param notificationIds 알림 ID 리스트
     */
    @Transactional
    public void markAsDeleted(Long userId, List<Long> notificationIds) {
        List<UserNotification> userNotifications = userNotificationRepository.findByIdInAndUserIdAndNotDeleted(notificationIds, userId);
        
        if (userNotifications.isEmpty()) {
            throw new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        
        userNotifications.forEach(UserNotification::markAsDeleted);
        userNotificationRepository.saveAll(userNotifications);
    }
}
