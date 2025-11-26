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

    public static RoomMember createMember(ChatRoom room, User user, ChatUserRole role) {
    RoomMember newMember = new RoomMember();
    newMember.room = room;
    newMember.user = user;
    newMember.role = role != null ? role : ChatUserRole.MEMBER;
    newMember.isExit = false;     // 생성 시 기본 규칙
    newMember.lastReadId = null;

    return newMember;
    }

    public void join() {
        this.isExit = false;
    }

    public void exit() {
        this.isExit = true;
    }
}