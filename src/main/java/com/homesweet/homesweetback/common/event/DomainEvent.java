package com.homesweet.homesweetback.common.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 도메인 이벤트
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
@Getter
public abstract class DomainEvent {
    private final String eventId;
    private final LocalDateTime occurredAt;
    private final String eventType;

    protected DomainEvent(String eventType) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.eventType = eventType;
    }
}
