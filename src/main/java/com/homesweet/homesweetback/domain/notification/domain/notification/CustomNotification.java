package com.homesweet.homesweetback.domain.notification.domain.notification;

import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 커스텀 알림 클래스
 * 
 * 템플릿을 사용하지 않는 커스텀 알림을 위한 클래스입니다.
 * 
 * @author dogyungkim
 */
@Getter
public class CustomNotification implements TemplateNotification {
    private final String title;
    private final String content;
    private final String redirectUrl;
    private final Map<String, Object> contextData;
    
    private final NotificationTemplateType eventType = NotificationTemplateType.CUSTOM;
    
    @Builder
    public CustomNotification(String title, String content, String redirectUrl, Map<String, Object> contextData) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required for CUSTOM notification");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is required for CUSTOM notification");
        }
        if (redirectUrl == null || redirectUrl.isBlank()) {
            throw new IllegalArgumentException("redirectUrl is required for CUSTOM notification");
        }
        if (contextData == null) {
            throw new IllegalArgumentException("contextData is required for CUSTOM notification");
        }
        
        this.title = title;
        this.content = content;
        this.redirectUrl = redirectUrl;
        this.contextData = contextData;
    }
    
    @Override
    public NotificationTemplateType getEventType() {
        return eventType;
    }
    
    @Override
    public Map<String, Object> toMap() {
        return contextData;
    }
}
