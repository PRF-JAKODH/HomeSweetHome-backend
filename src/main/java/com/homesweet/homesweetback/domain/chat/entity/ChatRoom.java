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

    @Column(name = "pair_key", length = 100)
    private String pairKey; //

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_message_id", nullable = true)
    private Long lastMessageId;

    @Column(name = "thumbnail_url", nullable = true, length = 100)
    private String thumbnailUrl;

}
