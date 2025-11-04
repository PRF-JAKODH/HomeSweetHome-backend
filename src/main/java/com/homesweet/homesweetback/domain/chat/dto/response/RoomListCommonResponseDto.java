package com.homesweet.homesweetback.domain.chat.dto.response;

import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RoomListCommonResponseDto(
        Long roomId,
        String roomName,
        ChatRoomType roomType,
        String thumbnailUrl,
        String lastMessage,
        LocalDateTime lastMessageAt,
        Long memberCount,
        Boolean lastMessageIsRead
) { }
