package com.homesweet.homesweetback.domain.search.chat.controller.response;

import com.homesweet.homesweetback.domain.search.chat.repository.document.ChatRoomDocument;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 단체 채팅방 조회 및 검색 응답 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
@Builder
public record ChatRoomSearchResponse(
        Long chatRoomId,
        String name,
        String thumbnailUrl,
        LocalDateTime createdAt
) {
    public static ChatRoomSearchResponse from(ChatRoomDocument doc) {
        return ChatRoomSearchResponse.builder()
                .chatRoomId(doc.getChatRoomId())
                .name(doc.getChatRoomName())
                .thumbnailUrl(doc.getThumbnailUrl())
                .createdAt(doc.getCreatedAt())
                .build();
    }

}
