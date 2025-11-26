package com.homesweet.homesweetback.domain.chat.event;

import com.homesweet.homesweetback.domain.chat.dto.response.JoinRoomResponse;
import com.homesweet.homesweetback.domain.chat.dto.response.RoomMemberResponse;
import jakarta.persistence.criteria.Join;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component // 또는 @Service
@RequiredArgsConstructor
public class ChatRoomEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 채팅방 멤버 입장 이벤트를 발행하고 브로드캐스트합니다.
     */
    public void publishMemberJoinedEvent(Long roomId, RoomMemberResponse memberResponse) {
        eventPublisher.publishEvent(
                new ChatRoomDataUpdateEvent(
                        roomId,
                        UpdateType.MEMBER_JOINED,
                        JoinRoomResponse.builder()
                )
        );
    }

    /**
     * (옵션) 퇴장 로직도 분리하여 일관성을 유지할 수 있습니다.
     */
    public void publishMemberLeftEvent(Long roomId, Map<String, Object> exitData) {
        eventPublisher.publishEvent(
                new ChatRoomDataUpdateEvent(
                        roomId,
                        UpdateType.MEMBER_LEFT,
                        exitData
                )
        );
    }
}