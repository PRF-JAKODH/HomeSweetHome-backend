package com.homesweet.homesweetback.domain.chat.dto;

import com.homesweet.homesweetback.domain.chat.entity.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageDto(
        Long messageId,
        Long roomId,
        Long senderId,
        String content,
        LocalDateTime sentAt,
        String senderName,
        String profileImageUrl


) {
    public static ChatMessageDto from(ChatMessage entity, String senderName, String profileImgUrl) {
        return new ChatMessageDto(
                entity.getId(),
                entity.getRoom().getId(),
                entity.getSender().getId(),
                entity.getContent(),
                entity.getSentAt(),
                senderName,
                profileImgUrl
        );
    }

}
