package com.homesweet.homesweetback.domain.notification.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;

@DisplayName("ChatNotification 테스트")
public class ChatNotificationTest {

    @Test
    @DisplayName("NewMessage 생성 테스트_성공")
    void testCreateNewMessage() {
        // Given
        ChatNotification.NewMessage newMessage = ChatNotification.NewMessage.builder()
            .userName("홍길동")
            .roomId(12345L)
            .roomName("채팅방 이름")
            .message("안녕하세요")
            .build();

        // Then
        assertThat(newMessage.getUserName()).isEqualTo("홍길동");
        assertThat(newMessage.getRoomId()).isEqualTo(12345L);
        assertThat(newMessage.getRoomName()).isEqualTo("채팅방 이름");
        assertThat(newMessage.getMessage()).isEqualTo("안녕하세요");
        assertThat(newMessage.toMap()).isEqualTo(Map.of("userName", "홍길동", "roomId", 12345L, "roomName", "채팅방 이름", "message", "안녕하세요"));
        assertThat(newMessage.getEventType()).isEqualTo(NotificationTemplateType.NEW_MESSAGE);
    }

    @Test
    @DisplayName("NewMessage 생성 테스트_실패_userName_null")
    void testCreateNewMessage_Failure_UserNameNull() {
        // When & Then
        assertThatThrownBy(() -> ChatNotification.NewMessage.builder()
            .userName(null)
            .roomId(12345L)
            .roomName("채팅방 이름")
            .message("안녕하세요")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("NewMessage 생성 테스트_실패_roomId_null")
    void testCreateNewMessage_Failure_RoomIdNull() {
        // When & Then
        assertThatThrownBy(() -> ChatNotification.NewMessage.builder()
            .userName("홍길동")
            .roomId(null)
            .roomName("채팅방 이름")
            .message("안녕하세요")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("NewMessage 생성 테스트_실패_roomName_null")
    void testCreateNewMessage_Failure_RoomNameNull() {
        // When & Then
        assertThatThrownBy(() -> ChatNotification.NewMessage.builder()
            .userName("홍길동")
            .roomId(12345L)
            .roomName(null)
            .message("안녕하세요")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("NewMessage 생성 테스트_실패_message_null")
    void testCreateNewMessage_Failure_MessageNull() {
        // When & Then
        assertThatThrownBy(() -> ChatNotification.NewMessage.builder()
            .userName("홍길동")
            .roomId(12345L)
            .roomName("채팅방 이름")
            .message(null)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }
}

