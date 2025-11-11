package com.homesweet.homesweetback.domain.chat.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record GroupChatDetailResponse(
        Long roomId,
        String roomName,
        String roomThumbnailUrl,
        Integer memberCount,
        List<MemberInfo> participants

) {
    @Builder
    public record MemberInfo(
            Long userId,
            String userName,
            String profileUrl
    ) {}
}
