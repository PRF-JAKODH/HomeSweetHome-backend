package com.homesweet.homesweetback.domain.chat.dto.response;

import lombok.Builder;

// 입장 시 응답
@Builder
public record JoinRoomResponse(
        Long roomId,
        String roomName,
        RoomMemberResponse memberInfo,
        JoinType joinType
) {
}
