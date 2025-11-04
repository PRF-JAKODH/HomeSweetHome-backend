package com.homesweet.homesweetback.domain.chat.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateGroupRoomRequest {

    @NotNull
    private Long ownerId;

    @NotNull
    private String roomName;

    @NotNull
    private String roomThumbnailUrl;



}
