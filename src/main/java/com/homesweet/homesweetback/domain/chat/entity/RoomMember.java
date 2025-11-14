package com.homesweet.homesweetback.domain.chat.entity;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatUserRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "room_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_member_id", nullable = false)
    private Long id;

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @JoinColumn(name = "room_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private ChatRoom room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private ChatUserRole role;

    @Column(nullable = false)
    private boolean isExit = false;

    @Column(name = "last_read_message_id", nullable = true)
    private Long lastReadId;

    @Builder
    private RoomMember(User user, ChatRoom room, ChatUserRole role,
                       Boolean isExit, Long lastReadId) {
        this.user = user;
        this.room = room;
        this.role = role != null ? role : ChatUserRole.MEMBER;
        this.isExit = isExit != null ? isExit : false;
        this.lastReadId = lastReadId;
    }

    public static RoomMember createMember(ChatRoom room, User user, ChatUserRole role) {
        return RoomMember.builder()
                .room(room)
                .user(user)
                .role(role != null ? role : ChatUserRole.MEMBER)
                .isExit(false)
                .build();
    }

    public void join() {
        this.isExit = false;
    }

    public void exit() {
        this.isExit = true;
    }

    public void updateLastReadMessageId(Long lastReadMessageId) {
        this.lastReadId = lastReadMessageId;
    }

    public boolean isActive() {
        return !this.isExit;
    }
}