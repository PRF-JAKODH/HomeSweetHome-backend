package com.homesweet.homesweetback.domain.chat.dto.response;

import com.homesweet.homesweetback.domain.chat.dto.ChatMessageDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(staticName = "of")
@Builder
public class RoomEnterResponse {
    private ChatRoomDetailResponse roomInfo;
    private PreMessageResponse preMessages;
}
