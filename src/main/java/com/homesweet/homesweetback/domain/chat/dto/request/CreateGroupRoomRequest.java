package com.homesweet.homesweetback.domain.chat.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

public record CreateGroupRoomRequest (

    @NotNull(message = "방 생성자는 필수입니다.")
    Long ownerId,

    @NotBlank(message = "방 이름은 필수입니다.")
    String roomName,

    MultipartFile roomThumbnailUrl

) {}
