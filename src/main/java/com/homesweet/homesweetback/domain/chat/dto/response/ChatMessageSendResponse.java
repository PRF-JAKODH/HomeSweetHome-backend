package com.homesweet.homesweetback.domain.chat.dto.response;

import com.homesweet.homesweetback.domain.chat.entity.ChatMessage;
import com.homesweet.homesweetback.domain.chat.entity.enums.MessageType;

import java.time.LocalDateTime;

public record ChatMessageSendResponse(
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

    public static ChatMessageSendResponse from(ChatMessage message, Long currentUserId) {
        return new ChatMessageSendResponse(
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

    public static ChatMessageSendResponse from(
            ChatMessage message,
            Long roomId,
            Long senderId,
            String senderName,
            String senderProfileImg,
            Long currentUserId
    ) {
        return new ChatMessageSendResponse(
                message.getId(),
                roomId,
                senderId,
                senderName,
                senderProfileImg,
                message.getMessageType(),
                message.getContent(),
                message.getSentAt(),
                senderId.equals(currentUserId)
        );
    }
}