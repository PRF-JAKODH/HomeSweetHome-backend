package com.homesweet.homesweetback.domain.chat.dto.response;

import com.homesweet.homesweetback.domain.chat.entity.ChatMessage;
import com.homesweet.homesweetback.domain.chat.entity.enums.MessageType;

import java.time.LocalDateTime;

public record ChatMessageResponse (
        Long messageId,
        Long roomId,
        Long senderId,
        String senderName,
        String senderProfileImg,
        MessageType messageType,
        String content,
        LocalDateTime sentAt,
        Boolean isRead
) {

    public static ChatMessageResponse from(ChatMessage message, Long currentUserId) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRoom().getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getSender().getProfileImageUrl(),
                message.getMessageType(),
                message.getContent(),
                message.getSentAt(),
                message.getSender().getId().equals(currentUserId)
        );
    }

}
