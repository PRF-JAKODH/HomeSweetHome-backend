package com.homesweet.homesweetback.domain.chat.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id", nullable = false)
    private Long id;

    @Pattern(regexp = "INDIVIDUAL|GROUP", message = "채팅방 타입은 INDIVIDUAL 또는 GROUP 이어야 합니다.")
    @Column(nullable = false, length = 100)
    private String type;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_message_id", nullable = false)
    private Long lastMessageId;

    @Column(name = "thumbnail_url", nullable = false, length = 100)
    private String thumbnailUrl;



}
