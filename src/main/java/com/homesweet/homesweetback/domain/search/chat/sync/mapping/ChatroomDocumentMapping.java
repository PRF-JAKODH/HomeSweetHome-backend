package com.homesweet.homesweetback.domain.search.chat.sync.mapping;

import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.search.chat.repository.document.ChatRoomDocument;
import org.springframework.stereotype.Component;

/**
 * 채팅방 엘라스틱 매핑 클래스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
@Component
public class ChatroomDocumentMapping {

    public ChatRoomDocument convertToDocument(ChatRoom chatRoom) {

        return ChatRoomDocument.builder()
                .chatRoomId(chatRoom.getId())
                .chatRoomName(chatRoom.getName())
                .thumbnailUrl(chatRoom.getThumbnailUrl())
                .isDeleted(chatRoom.getIsDeleted())
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }
}
