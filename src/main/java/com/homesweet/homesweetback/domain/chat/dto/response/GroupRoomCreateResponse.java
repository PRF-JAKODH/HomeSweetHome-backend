package com.homesweet.homesweetback.domain.chat.dto.response;

import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import lombok.Builder;

// 채팅방 생성 응답 (비회원 그룹채팅 전제 조회 경우)
@Builder
public record GroupRoomCreateResponse(
    Long ownerId,
    Long roomId,
    String roomName,
    ChatRoomType type,
    String roomThumbnailUrl
){


}
