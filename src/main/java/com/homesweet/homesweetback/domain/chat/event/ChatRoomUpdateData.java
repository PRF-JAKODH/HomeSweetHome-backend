package com.homesweet.homesweetback.domain.chat.event;

public record ChatRoomUpdateData(
        Long roomId,
        String updateType,  // "MEMBER_JOINED" or "MEMBER_LEFT"
        Object data,        // RoomMemberResponse, JoinRoomResponse, ExitData 등
        String occurredAt   // ISO 8601 형식 문자열
) {
}
