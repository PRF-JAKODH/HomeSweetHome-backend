package com.homesweet.homesweetback.domain.chat.dto.request;

public record ChatReadRequest (
        Long roomId,
        Long lastReadMessageId
) {}

