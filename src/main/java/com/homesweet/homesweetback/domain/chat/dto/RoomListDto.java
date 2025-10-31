package com.homesweet.homesweetback.domain.chat.dto;

import java.time.Instant;
import java.time.LocalDateTime;

public record RoomListDto (
        Long roomId,
        Long partnerId,
        String partnerName,
        String roomName,
        String thumbnailUrl,
        String lastMessage,                     // 마지막 메시지 미리보기
        LocalDateTime lastMessageAt,            // 보낸 시간 (최신순 정렬 시)
        Long lastMessageId,                     // 메세지 새부조회 연결
        Long lastReadMessageId                  // 읽음, 안읽음
//    boolean hasUnread,
//    long unreadCount
) {}
