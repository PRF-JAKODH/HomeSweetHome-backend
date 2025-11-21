package com.homesweet.homesweetback.domain.chat.dto.response;

import lombok.Builder;

// NewMemberInfoResponse
@Builder
public record MemberInfo (
        Long userId,
        String userName,
        String profileUrl
){
}
