package com.homesweet.homesweetback.domain.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateIndividualRoomRequest {
    @NotNull
    private Long meId;

    @NotNull
    private Long targetId;
}
