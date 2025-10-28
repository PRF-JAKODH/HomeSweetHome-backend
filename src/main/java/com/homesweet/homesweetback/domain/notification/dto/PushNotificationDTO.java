package com.homesweet.homesweetback.domain.notification.dto;

import java.util.Map;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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

    public String toJson() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert to JSON", e);
        }
    }
}   