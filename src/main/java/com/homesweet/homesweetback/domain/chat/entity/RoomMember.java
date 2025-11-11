package com.homesweet.homesweetback.domain.chat.entity;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatUserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_member_id",nullable = false)
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

    private Boolean isExit;

    @Column(name = "last_read_message_id", nullable = true)
    private Long lastReadId;


    // ⚠setter는 막는다 (상태를 직접 바꾸지 못하게)
    protected void setExit(boolean exit) {
        this.isExit = exit;
    }

    public void join() {
        if (this.isExit) {               // 퇴장 상태일 때만 입장 가능
            this.isExit = false;
        }
    }
    public void exit() {

        this.isExit = true;
    }
    public boolean isActive() {
        // 퇴장 상태(isExit이 true)가 아니면 활성 상태입니다.
        return !this.isExit;
    }
    public void updateLastReadMessageId(Long lastReadMessageId) {
        this.lastReadId = lastReadMessageId;
    }

    // 새로운 멤버 생성.
    public static RoomMember createMember(ChatRoom room, User user, ChatUserRole role) {
        RoomMember member = new RoomMember();
        member.room = room;
        member.user = user;
        member.role = (role != null) ? role : ChatUserRole.MEMBER;
        member.isExit = false;
        member.lastReadId = null;
        return member;
    }
}


