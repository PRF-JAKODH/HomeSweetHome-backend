package com.homesweet.homesweetback.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class NotificationMessage {
    private Long userId;
    private PushNotificationDTO data;
}
