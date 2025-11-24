package com.homesweet.homesweetback.domain.chat.event;

import java.time.LocalDateTime;

public record WebSocketMessage(
        String type,
        Object data,
        LocalDateTime timestamp
) {
    // (timestamp 자동 설정)
    public WebSocketMessage(String type, Object data) {
        this(type, data, LocalDateTime.now());
    }
}
