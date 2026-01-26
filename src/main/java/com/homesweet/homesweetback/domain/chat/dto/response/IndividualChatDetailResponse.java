package com.homesweet.homesweetback.domain.chat.dto.response;

import lombok.Builder;

@Builder
public record IndividualChatDetailResponse(
        Long roomId,
        Long partnerId,
        String partnerName,
        String partnerProfileImageUrl
) {
}
