package com.homesweet.homesweetback.domain.chat.event;

public record MemberLeftResponseDTO(
        Long roomId,
        Long userId
) {
}
