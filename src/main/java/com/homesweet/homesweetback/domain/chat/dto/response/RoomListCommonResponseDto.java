package com.homesweet.homesweetback.domain.chat.dto.response;

import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;


@Builder
public record RoomListCommonResponseDto(
        Long roomId,
        String roomName,
        ChatRoomType roomType,
        Long memberCount,

        // 상대방 정보
        Long partnerId,
        String partnerName,
        String thumbnailUrl,

        // 마지막 메세지 관련
        String lastMessage,
        LocalDateTime lastMessageAt,
        Long lastMessageId,
        Boolean lastMessageIsRead

) {
    public boolean getLastMessageIsRead() {
        return lastMessageIsRead;
    }
}
