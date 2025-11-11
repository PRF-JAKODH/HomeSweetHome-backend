package com.homesweet.homesweetback.domain.chat.mapper;

import com.homesweet.homesweetback.domain.chat.dto.request.CreateGroupRoomRequest;
import com.homesweet.homesweetback.domain.chat.dto.response.GroupRoomCreateResponse;
import com.homesweet.homesweetback.domain.chat.dto.response.GroupRoomListResponse;
import com.homesweet.homesweetback.domain.chat.entity.ChatMessage;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class ChatRoomMapper {

   /**
    * 엔터티 -> DTO
    */
    public GroupRoomCreateResponse toDto(ChatRoom chatRoom, Long ownerId) {
        return GroupRoomCreateResponse.builder()
                .ownerId(ownerId)
                .roomId(chatRoom.getId())
                .roomName(chatRoom.getName())
                .roomThumbnailUrl(chatRoom.getThumbnailUrl())
                .type(ChatRoomType.GROUP)
                .build();
    }

    /**
     * DTO → Entity
     * (클라이언트 요청 DTO → DB에 저장할 ChatRoom)
     */
    public ChatRoom toEntity(CreateGroupRoomRequest request, String thumbnailUrl) {
        return ChatRoom.builder()
                .name(request.roomName())
                .thumbnailUrl(thumbnailUrl)
                .type(ChatRoomType.GROUP)
                .build();
    }

    /**
     * 그룹 채팅방 Entity -> DTO 변환
     */
    public GroupRoomListResponse toGroupRoomListDto(
            ChatRoom room,
            ChatMessage lastMessage,
            Long memberCount) {

        String thumbnailUrl = room.getThumbnailUrl();

        String lastMessageContent = lastMessage != null ? lastMessage.getContent() : null;
        LocalDateTime lastMessageSentAt = lastMessage != null ? lastMessage.getSentAt() : null;

        return new GroupRoomListResponse(
                room.getId(),
                room.getName(),
                room.getType(),
                thumbnailUrl,
                memberCount,
                room.getLastMessage(),
                room.getLastMessageAt()
        );
    }

}
