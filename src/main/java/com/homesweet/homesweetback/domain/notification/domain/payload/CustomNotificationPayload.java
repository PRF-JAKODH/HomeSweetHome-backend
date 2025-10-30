package com.homesweet.homesweetback.domain.notification.domain.payload;

import java.util.Map;

import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;

@SupportsEventType(NotificationEventType.CUSTOM)
public class CustomNotificationPayload extends NotificationPayload {
    private Map<String, Object> contextData;

    @Override
    public Map<String, Object> toMap() {
        return contextData;
    }

    @Override
    protected void validateRequiredFields() {
        if (contextData == null || contextData.isEmpty()) {
            throw new IllegalArgumentException("contextData is required for CUSTOM notification");
        }
    }

    public CustomNotificationPayload(Map<String, Object> contextData) {
        this.contextData = contextData;
    }
}
