package com.homesweet.homesweetback.domain.chat.dto.response;

import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import java.time.LocalDateTime;

// 내가 속한 그룹 채팅방 목록 조회용
public record GroupRoomListResponse (
        // 방 기본정보
        Long roomId,
        String roomName,
        ChatRoomType type,
        String thumbnailUrl,

        // 목록에 들어갈 정보
        Long memberCount,
//        Boolean isRead,

        // 마지막 메세지 보여주기용
        String lastMessage,
        LocalDateTime lastMessageAt
){

}
