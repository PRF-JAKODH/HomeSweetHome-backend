package com.homesweet.homesweetback.domain.chat.dto;

public record ChatMessageDTO (
        Long id,
        Long roomId,
        Long senderId,
        String text,
        String createdAt
) {}
