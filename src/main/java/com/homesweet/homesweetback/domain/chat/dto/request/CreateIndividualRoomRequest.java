package com.homesweet.homesweetback.domain.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateIndividualRoomRequest {

    @NotNull(message = "대상 사용자 ID는 필수입니다.")
    private Long targetId;
}
