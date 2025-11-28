package com.homesweet.homesweetback.domain.chat.event.search;

import com.homesweet.homesweetback.common.event.DomainEvent;
import com.homesweet.homesweetback.domain.community.event.CommunityEvent;
import com.homesweet.homesweetback.domain.community.event.CommunityEventType;
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

    public static ChatroomEvent updated(Long communityId) {
        return new ChatroomEvent(communityId, ChatroomEventType.UPDATED);
    }

    public static ChatroomEvent deleted(Long communityId) {
        return new ChatroomEvent(communityId, ChatroomEventType.DELETED);
    }
}
