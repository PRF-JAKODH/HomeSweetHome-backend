package com.homesweet.homesweetback.domain.chat.dto.response;

import lombok.Builder;

/**
 * 채팅방 상세 정보 응답 DTO
 */
@Builder
public record ChatRoomDetailResponse(
        Long roomId,
        Long partnerId,
        String partnerName,
        String thumbnailUrl
) {}