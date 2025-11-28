package com.homesweet.homesweetback.domain.search.chat.event;

import com.homesweet.homesweetback.common.event.DomainEvent;
import lombok.Getter;

/**
 * 채팅방 이벤트
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
@Getter
public class ChatroomEvent extends DomainEvent {
    private final Long chatroomId;
    private final ChatroomEventType chatroomEventType;

    public ChatroomEvent(Long chatroomId, ChatroomEventType eventType) {
        super("chatroom." + eventType.name().toLowerCase());
        this.chatroomId = chatroomId;
        this.chatroomEventType = eventType;
    }

    public static ChatroomEvent created(Long chatroomId) {
        return new ChatroomEvent(chatroomId, ChatroomEventType.CREATED);
    }

    public static ChatroomEvent updated(Long chatroomId) {
        return new ChatroomEvent(chatroomId, ChatroomEventType.UPDATED);
    }

    public static ChatroomEvent deleted(Long chatroomId) {
        return new ChatroomEvent(chatroomId, ChatroomEventType.DELETED);
    }
}
