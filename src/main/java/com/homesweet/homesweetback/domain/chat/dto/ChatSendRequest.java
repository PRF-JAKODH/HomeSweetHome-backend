package com.homesweet.homesweetback.domain.chat.dto;

public record ChatSendRequest(
        Long roomId,
        String clientMsgId,
        Long senderId,
        String text
) {}
