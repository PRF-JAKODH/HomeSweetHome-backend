package com.homesweet.homesweetback.domain.notification.service.impl;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.homesweet.homesweetback.domain.notification.domain.event.CustomNotificationEvent;
import com.homesweet.homesweetback.domain.notification.domain.event.TemplateNotificationEvent;
import com.homesweet.homesweetback.domain.notification.domain.notification.CustomNotification;
import com.homesweet.homesweetback.domain.notification.domain.notification.TemplateNotification;
import com.homesweet.homesweetback.domain.notification.service.NotificationSendService;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class KafkaNotificationSendService implements NotificationSendService {

    private final KafkaTemplate<String, TemplateNotificationEvent> templateNotificationKafkaTemplate;
    private final KafkaTemplate<String, CustomNotificationEvent> customNotificationKafkaTemplate;

    @Override
    public void sendTemplateNotificationToSingleUser(Long userId, TemplateNotification notification) {
        templateNotificationKafkaTemplate.send("notification", new TemplateNotificationEvent(userId, notification));
    }

    @Override
    public void sendTemplateNotificationToMultipleUsers(List<Long> userIds, TemplateNotification notification) {
        templateNotificationKafkaTemplate.send("notification", new TemplateNotificationEvent(userIds, notification));
    }

    @Override
    public void sendCustomNotificationToSingleUser(Long userId, CustomNotification notification) {
        customNotificationKafkaTemplate.send("notification", new CustomNotificationEvent(userId, notification));
    }

    @Override
    public void sendCustomNotificationToMultipleUsers(List<Long> userIds, CustomNotification notification) {
        customNotificationKafkaTemplate.send("notification", new CustomNotificationEvent(userIds, notification));
    }
}