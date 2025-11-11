package com.homesweet.homesweetback.domain.chat.dto.response;

import com.homesweet.homesweetback.domain.chat.dto.ChatMessageDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.List;

// 이전 메세지 dto
@Getter
@AllArgsConstructor
@Builder
public class PreMessageResponse {
    private List<ChatMessageDto> messages;
    private boolean hasMore;

    public static PreMessageResponse of(List<ChatMessageDto> messages, boolean hasNext) {
        return new PreMessageResponse(messages, hasNext);
    }
}
