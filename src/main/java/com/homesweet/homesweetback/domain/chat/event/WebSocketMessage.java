package com.homesweet.homesweetback.domain.chat.event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record WebSocketMessage(
        String type,
        Object data,
        String timestamp
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;


    public WebSocketMessage(String type, Object data) {
        this(type, data, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
