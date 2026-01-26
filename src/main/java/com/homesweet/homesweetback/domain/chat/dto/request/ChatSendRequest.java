package com.homesweet.homesweetback.domain.chat.dto.request;

public record ChatSendRequest(
        Long senderId,          // 전송자 (본인)
        Long roomId,
        String content
) { }
