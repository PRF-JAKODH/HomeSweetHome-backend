package com.homesweet.homesweetback.domain.chat.service.Imp;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.chat.dto.response.ChatMessageSendResponse;
import com.homesweet.homesweetback.domain.chat.entity.ChatMessage;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import com.homesweet.homesweetback.domain.chat.entity.enums.MessageType;
import com.homesweet.homesweetback.domain.chat.repository.ChatMessageRepository;
import com.homesweet.homesweetback.domain.chat.repository.ChatRoomRepository;
import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
import com.homesweet.homesweetback.domain.chat.service.ChatMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("ChatMessageService 통합 테스트")
class ChatMessageServiceTest {

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private User sender;
    private ChatRoom chatRoom;
    private RoomMember roomMember;

    @BeforeEach
    void setUp() {
        // 실제 DB에 데이터 저장
        sender = User.builder()
                .name("맹구")                          // 필수
                .email("maenggu@test.com")            // 필수
                .provider(OAuth2Provider.GOOGLE)      // 필수 ← Enum으로!
                .role(UserRole.USER)                  // 필수 ← Enum으로!
                .profileImageUrl("test.png")          // 선택
                .build();
        sender = userRepository.save(sender);

        chatRoom = ChatRoom.builder()
                .id(1L)
                .type(ChatRoomType.INDIVIDUAL)
                .name("떡잎마을방범대 모임방")
                .createdAt(LocalDateTime.now())
                .build();
        chatRoom = chatRoomRepository.save(chatRoom);

        roomMember = RoomMember.builder()
                .user(sender)
                .room(chatRoom)
                .isExit(false)
                .build();
        roomMember = roomMemberRepository.save(roomMember);
    }

    @Test
    @DisplayName("[Integration] 정상 메시지 전송 - 실제 DB 저장 확인")
    void sendMessage_shouldSaveToDatabase() {
        // Given
        String content = "안녕, 짱구야";

        // When
        ChatMessageSendResponse response = chatMessageService.sendMessage(
                chatRoom.getId(),
                sender.getId(),
                content
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.roomId()).isEqualTo(chatRoom.getId());
        assertThat(response.senderId()).isEqualTo(sender.getId());
        assertThat(response.content()).isEqualTo(content);
        assertThat(response.senderName()).isEqualTo("맹구");

        // DB에 실제로 저장되었는지 확인
        ChatMessage savedMessage = chatMessageRepository.findById(response.messageId())
                .orElseThrow();
        assertThat(savedMessage.getContent()).isEqualTo(content);
        assertThat(savedMessage.getSender().getId()).isEqualTo(sender.getId());
        assertThat(savedMessage.getRoom().getId()).isEqualTo(chatRoom.getId());
        assertThat(savedMessage.getMessageType()).isEqualTo(MessageType.TEXT);

        // 채팅방의 마지막 메시지가 업데이트되었는지 확인
        ChatRoom updatedRoom = chatRoomRepository.findById(chatRoom.getId())
                .orElseThrow();
        assertThat(updatedRoom.getLastMessage()).isEqualTo(content);
        assertThat(updatedRoom.getLastMessageAt()).isNotNull();
    }

    @Test
    @DisplayName("[Integration] 존재하지 않는 채팅방 - 예외 발생")
    void sendMessage_shouldThrowException_whenRoomNotFound() {
        // Given
        Long invalidRoomId = 999L;

        // When & Then
        assertThatThrownBy(() -> chatMessageService.sendMessage(
                invalidRoomId,
                sender.getId(),
                "테스트"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 채팅방입니다.");
    }

    @Test
    @DisplayName("[Integration] 채팅방 멤버가 아님 - 예외 발생")
    void sendMessage_shouldThrowException_whenNotMember() {
        // Given
        User nonMember = User.builder()
                .name("철수")
                .email("chulsoo@test.com")
                .provider(OAuth2Provider.KAKAO)   // ✅ Enum
                .role(UserRole.USER)              // ✅ Enum
                .profileImageUrl("chulsoo.png")
                .build();
        nonMember = userRepository.save(nonMember);

        // When & Then
        Long nonMemberId = nonMember.getId();
        assertThatThrownBy(() -> chatMessageService.sendMessage(
                chatRoom.getId(),
                nonMemberId,
                "테스트"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("채팅방 멤버가 아니거나 이미 퇴장한 사용자입니다.");
    }

    @Test
    @DisplayName("[Integration] 퇴장한 사용자 - 예외 발생")
    void sendMessage_shouldThrowException_whenUserExited() {
        // Given
        roomMember.exit();  // 퇴장 처리
        roomMemberRepository.save(roomMember);

        // When & Then
        assertThatThrownBy(() -> chatMessageService.sendMessage(
                chatRoom.getId(),
                sender.getId(),
                "테스트"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("채팅방 멤버가 아니거나 이미 퇴장한 사용자입니다.");
    }

    @Test
    @DisplayName("[Integration] 여러 메시지 연속 전송")
    void sendMessage_shouldHandleMultipleMessages() {
        // Given
        String message1 = "첫 번째 메시지";
        String message2 = "두 번째 메시지";
        String message3 = "세 번째 메시지";

        // When
        ChatMessageSendResponse response1 = chatMessageService.sendMessage(
                chatRoom.getId(), sender.getId(), message1);
        ChatMessageSendResponse response2 = chatMessageService.sendMessage(
                chatRoom.getId(), sender.getId(), message2);
        ChatMessageSendResponse response3 = chatMessageService.sendMessage(
                chatRoom.getId(), sender.getId(), message3);

        // Then
        assertThat(response1.content()).isEqualTo(message1);
        assertThat(response2.content()).isEqualTo(message2);
        assertThat(response3.content()).isEqualTo(message3);

        // 마지막 메시지가 채팅방에 반영되었는지 확인
        ChatRoom updatedRoom = chatRoomRepository.findById(chatRoom.getId())
                .orElseThrow();
        assertThat(updatedRoom.getLastMessage()).isEqualTo(message3);
    }

//    @Test
//    @DisplayName("[Integration] 메시지 전송 권한 확인")
//    void canSendMessage_shouldWorkWithRealDatabase() {
//        // When
//        boolean canSend = chatMessageService.canSendMessage(sender.getId(), chatRoom.getId());
//
//        // Then
//        assertThat(canSend).isTrue();
//
//        // 다른 사용자는 권한이 없어야 함
//        User otherUser = User.builder()
//                .name("영희")
//                .email("younghee@test.com")
//                .build();
//        otherUser = userRepository.save(otherUser);
//
//        boolean cannotSend = chatMessageService.canSendMessage(otherUser.getId(), chatRoom.getId());
//        assertThat(cannotSend).isFalse();
//    }
}