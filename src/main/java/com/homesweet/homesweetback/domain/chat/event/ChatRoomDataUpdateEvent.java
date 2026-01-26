package com.homesweet.homesweetback.domain.chat.event;

import com.homesweet.homesweetback.domain.chat.dto.response.JoinRoomResponse;

import java.time.LocalDateTime;

/**
 * Spring 내부 이벤트 (백엔드 내부용)
 * ApplicationEventPublisher로 발행 → ChatRoomEventListener에서 수신
 */
public record ChatRoomDataUpdateEvent(
        Long roomId,
        UpdateType updateType,
        Object data,
        LocalDateTime occurredAt
) {
    public ChatRoomDataUpdateEvent(Long roomId, UpdateType updateType, Object data) {
        this(roomId, updateType, data, LocalDateTime.now());
    }
}
