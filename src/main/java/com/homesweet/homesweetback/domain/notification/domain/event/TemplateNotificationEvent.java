package com.homesweet.homesweetback.domain.notification.domain.event;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.notification.domain.notification.TemplateNotification;
import com.homesweet.homesweetback.domain.notification.exception.NotificationException;

import java.util.List;

/**
 * 템플릿 기반 알림 이벤트
 * 
 * TemplateNotification을 통해 알림을 전송합니다.
 * 단일 사용자 또는 다수 사용자 모두 지원합니다.
 * 
 * @author dogyungkim
 */
public record TemplateNotificationEvent(
        List<Long> userIds,
        TemplateNotification notification) {
    public TemplateNotificationEvent {
        if (userIds == null || userIds.isEmpty()) {
            throw new NotificationException(ErrorCode.DATA_MISSING);
        }
        if (notification == null) {
            throw new NotificationException(ErrorCode.DATA_MISSING);
        }
    }

    /**
     * 단일 사용자용 생성자
     */
    public TemplateNotificationEvent(Long userId, TemplateNotification notification) {
        this(List.of(userId), notification);
    }
}
