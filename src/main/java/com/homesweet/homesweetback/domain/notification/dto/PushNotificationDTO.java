package com.homesweet.homesweetback.domain.notification.dto;

import java.util.Map;
import java.time.LocalDateTime;

import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PushNotificationDTO {
    Long notificationId;
    String title;
    String content;
    String redirectUrl;
    Map<String, Object> contextData;
    boolean isRead;
    NotificationCategoryType categoryType;
    LocalDateTime createdAt;

}