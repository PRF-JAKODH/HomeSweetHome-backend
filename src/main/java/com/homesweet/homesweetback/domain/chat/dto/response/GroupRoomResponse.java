package com.homesweet.homesweetback.domain.chat.dto.response;

import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import lombok.Builder;
import lombok.Getter;

@Builder
public record GroupRoomResponse (
    Long ownerId,
    Long roomId,
    String roomName,
    ChatRoomType type,
    String roomThumbnailUrl
){


}
