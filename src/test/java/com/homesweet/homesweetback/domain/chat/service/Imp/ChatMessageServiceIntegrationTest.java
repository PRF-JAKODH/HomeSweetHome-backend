package com.homesweet.homesweetback.domain.chat.service.Imp;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
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
class ChatMessageServiceIntegrationTest {

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
        // 사용자
        sender = User.builder()
                .name("맹구")
                .email("maenggu@test.com")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .profileImageUrl("test.png")
                .build();
        sender = userRepository.save(sender);

        // 채팅방
        chatRoom = ChatRoom.builder()
                .type(ChatRoomType.INDIVIDUAL)
                .name("떡잎마을방범대 모임방")
                .createdAt(LocalDateTime.now())
                .build();
        chatRoom = chatRoomRepository.save(chatRoom);

        // 멤버
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
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("[Integration] 채팅방 멤버가 아님 - 예외 발생")
    void sendMessage_shouldThrowException_whenNotMember() {
        // Given
        User nonMember = User.builder()
                .name("철수")
                .email("chulsoo@test.com")
                .provider(OAuth2Provider.KAKAO)
                .role(UserRole.USER)
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
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_MEMBER_NOT_FOUND);
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
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("[Integration] 여러 메시지 연속 전송")
    void sendMessage_shouldHandleMultipleMessages() {
        // Given
        String message1 = "첫 번째 메시지";
        String message2 = "두 번째 메시지";
        String message3 = "세 번째 메시지";

        // When
        chatMessageService.sendMessage(chatRoom.getId(), sender.getId(), message1);
        chatMessageService.sendMessage(chatRoom.getId(), sender.getId(), message2);
        ChatMessageSendResponse response3 = chatMessageService.sendMessage(
                chatRoom.getId(), sender.getId(), message3);

        // Then
        // 첫 번째, 두 번째 메시지는 응답 검증 없이 실행만 확인하고, 세 번째 메시지만 검증
        assertThat(response3.content()).isEqualTo(message3);

        // 마지막 메시지가 채팅방에 반영되었는지 확인
        ChatRoom updatedRoom = chatRoomRepository.findById(chatRoom.getId())
                .orElseThrow();
        assertThat(updatedRoom.getLastMessage()).isEqualTo(message3);
    }

    @Test
    @DisplayName("[Integration] 메시지 전송 권한 확인")
    void canSendMessage_shouldWorkWithRealDatabase() {
        // When
        boolean canSend = chatMessageService.canSendMessage(sender.getId(), chatRoom.getId());

        // Then
        assertThat(canSend).isTrue();

        // 다른 사용자는 권한이 없어야 함
        User otherUser = User.builder()
                .name("맹구")
                .email("meanggoo@test.com")
                .provider(OAuth2Provider.GOOGLE) // 기본값 설정
                .role(UserRole.USER) // 기본값 설정
                .build();
        otherUser = userRepository.save(otherUser);

        boolean cannotSend = chatMessageService.canSendMessage(otherUser.getId(), chatRoom.getId());
        assertThat(cannotSend).isFalse();
    }
}