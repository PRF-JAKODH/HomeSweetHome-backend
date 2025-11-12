package com.homesweet.homesweetback.domain.chat.dto;

import lombok.Builder;

@Builder
public record RoomDto (

    Long roomId,
    String type,   // "INDIVIDUAL" / "GROUP"
    String name,
    String partnerName,
    String pairKey,  // 1:1일 때만 값 존재
    boolean reused
){ }
