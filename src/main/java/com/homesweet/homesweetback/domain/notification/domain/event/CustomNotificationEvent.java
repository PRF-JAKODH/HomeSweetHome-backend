package com.homesweet.homesweetback.domain.notification.domain.event;

import java.util.List;

import com.homesweet.homesweetback.domain.notification.domain.notification.CustomNotification;

/**
 * 커스텀 알림 이벤트
 * 
 * @author dogyungkim
 */
public record CustomNotificationEvent(
    List<Long> userIds,
    CustomNotification notification
) {
    public CustomNotificationEvent {
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("userIds는 필수이며 비어있을 수 없습니다.");
        }
        if (notification == null) {
            throw new IllegalArgumentException("notification은 필수입니다.");
        }
    }
    
    /**
     * 단일 사용자용 생성자
     */
    public CustomNotificationEvent(
        Long userId,
        CustomNotification notification
    ) {
        this(List.of(userId), notification);
    }
}

