package com.homesweet.homesweetback.domain.chat.event;

import java.time.LocalDateTime;

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
