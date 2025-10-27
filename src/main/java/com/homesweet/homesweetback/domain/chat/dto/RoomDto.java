package com.homesweet.homesweetback.domain.chat.dto;


import lombok.*;

// 추후 레코드 활용
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDto {

    private Long roomId;
    private String type;     // "INDIVIDUAL" / "GROUP"
    private String name;
    private String pairKey;  // 1:1일 때만 값 존재
    private boolean reused;

}
