package com.homesweet.homesweetback.domain.chat.dto.response;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import java.time.LocalDateTime;

// 개인 채팅방 목록 조회 list
public record IndividualRoomListResponse(
        Long roomId,
        ChatRoomType roomType,
        Long memberCount,

        Long partnerId,
        String partnerName,
        String thumbnailUrl,

        // 마지막 메세지 관련
        String lastMessage,
        LocalDateTime lastMessageAt
) {
   public static IndividualRoomListResponse toDto(ChatRoom room, User partner) {
       return new IndividualRoomListResponse(
               room.getId(),
               room.getType(),
               2L,
               partner.getId(),
               partner.getName(),
               partner.getProfileImageUrl(),
               room.getLastMessage(),
               room.getLastMessageAt()
       );
   }

}
