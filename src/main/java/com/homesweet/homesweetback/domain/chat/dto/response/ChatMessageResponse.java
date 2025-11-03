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
                message.getSender().getProfileImageUrl(),  // User 엔티티에 이 필드가 있어야 해요
                message.getMessageType(),
                message.getContent(),
                message.getSentAt(),
                message.getSender().getId().equals(currentUserId)  // 본인이 보낸 메시지는 읽음 처리
        );
    }

}
