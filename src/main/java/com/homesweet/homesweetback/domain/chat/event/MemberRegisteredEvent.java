package com.homesweet.homesweetback.domain.chat.event;

public class MemberRegisteredEvent {

    private final Long roomId;

    public MemberRegisteredEvent(Long roomId) {
        this.roomId = roomId;
    }

    public Long getRoomId() {
        return roomId;
    }
}
