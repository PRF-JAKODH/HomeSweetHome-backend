package com.homesweet.homesweetback.domain.chat.mapper;

import com.homesweet.homesweetback.domain.chat.dto.request.CreateGroupRoomRequest;
import com.homesweet.homesweetback.domain.chat.dto.response.GroupRoomResponse;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import org.springframework.stereotype.Component;

@Component
public class ChatRoomMapper {

   /**
    * 엔터티 -> DTO
    */
    public GroupRoomResponse toDto(ChatRoom chatRoom, Long ownerId) {
        return GroupRoomResponse.builder()
                .ownerId(ownerId)
                .roomId(chatRoom.getId())       // ✅ DB 저장 후 자동 생성된 값
                .roomName(chatRoom.getName())
                .roomThumbnailUrl(chatRoom.getThumbnailUrl())
                .type(ChatRoomType.GROUP)
                .build();
    }

    /**
     * DTO → Entity
     * (클라이언트 요청 DTO → DB에 저장할 ChatRoom)
     */
    public ChatRoom toEntity(CreateGroupRoomRequest request) {
        return ChatRoom.builder()
                .name(request.getRoomName())
                .thumbnailUrl(request.getRoomThumbnailUrl())
                .type(ChatRoomType.GROUP)
                .build();
    }


}
