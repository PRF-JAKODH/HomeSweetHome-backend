package com.homesweet.homesweetback.domain.chat.dto.response;

import lombok.Builder;

import java.util.List;

// 입장 시 응답
@Builder
public record JoinRoomResponse(
        Long roomId,
        String roomName,
        List<RoomMemberResponse> memberInfo,
        List<JoinType> joinType
) {
}
