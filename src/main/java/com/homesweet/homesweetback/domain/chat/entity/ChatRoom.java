package com.homesweet.homesweetback.domain.chat.entity;

import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_room",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_room_type_pair",
                        columnNames = {"type", "pair_key"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private ChatRoomType type;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "pair_key", nullable = true, length = 100)
    private String pairKey; //

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "thumbnail_url", length = 500 )
    private String thumbnailUrl;

    @Column(name = "last_message", nullable = true)
    private String lastMessage;

    @Column(name = "last_message_sent_at", nullable = true)
    private LocalDateTime lastMessageAt;

    /**
     *  삭제된 방인지 확인
     */
    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /**
     * 채팅방 소프트 삭제
     */
    public void delete() {

        this.isDeleted = true;
    }

    /**
     * 마지막 메시지 정보 업데이트
     */
    public void updateLastMessage(String message, LocalDateTime sentAt) {
        this.lastMessage = message;
        this.lastMessageAt = sentAt;
    }

}
