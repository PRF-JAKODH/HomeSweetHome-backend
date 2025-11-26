package com.homesweet.homesweetback.domain.chat.dto.response;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import lombok.Builder;

// NewMemberInfoResponse
@Builder
public record RoomMemberResponse(
        Long userId,
        String userName,
        String profileUrl
){
    public static RoomMemberResponse from(RoomMember member) {
        User user = member.getUser();

        return RoomMemberResponse.builder()
                .userId(user.getId())
                .userName(user.getName())
                .profileUrl(user.getProfileImageUrl())
                .build();
    }
}
