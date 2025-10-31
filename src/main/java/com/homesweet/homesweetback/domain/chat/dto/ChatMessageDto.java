package com.homesweet.homesweetback.domain.chat.dto;

import java.time.LocalDateTime;

public record ChatMessageDto(
        Long id,
        Long roomId,
        Long senderId,
        String text,
        LocalDateTime sentaAt
) {}
