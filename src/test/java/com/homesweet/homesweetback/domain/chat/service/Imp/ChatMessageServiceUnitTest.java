package com.homesweet.homesweetback.domain.chat.service.Imp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.chat.dto.response.ChatMessageSendResponse;
import com.homesweet.homesweetback.domain.chat.entity.ChatMessage;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatUserRole;
import com.homesweet.homesweetback.domain.chat.entity.enums.MessageType;
import com.homesweet.homesweetback.domain.chat.repository.ChatMessageRepository;
import com.homesweet.homesweetback.domain.chat.repository.ChatRoomRepository;
import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


/**
 * @author hygg0408e@gmail.com
 * @date 25. 11. 11.
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ChatMessageServiceUnitTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private RoomMemberRepository roomMemberRepository;

    @InjectMocks
    private ChatMessageServiceImpl service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User sender;
    private ChatRoom room;
    private RoomMember roomMember;
    private ChatMessage chatMessage;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new Jdk8Module());

        // Given - 테스트 데이터 준비
        sender = User.builder()
                .id(1L)
                .name("맹구씨")
                .profileImageUrl("https://test.com/profile.jpg")
                .build();

        room = ChatRoom.builder()
                .id(100L)
                .build();

        roomMember = RoomMember.builder()
                .room(room)
                .user(sender)
                .role(ChatUserRole.MEMBER)
                .isExit(false)
                .build();

        chatMessage = ChatMessage.builder()
                .id(1L)
                .room(room)
                .sender(sender)
                .content("안녕 짱구씨")
                .messageType(MessageType.TEXT)
                .sentAt(LocalDateTime.now())
                .build();
    }


    @Test
    @DisplayName("[성공] WebSocket으로 전송되는 응답 DTO JSON 직렬화 테스트")
    void sendMessage_JsonSerialization() throws Exception {
        // Given
        Long roomId = 100L;
        Long senderId = 1L;
        String content = "안녕 짱구씨";

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(roomMemberRepository.findByUserIdAndRoomId(senderId, roomId)).willReturn(roomMember);
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(chatMessage);

        // When
        ChatMessageSendResponse response = service.sendMessage(roomId, senderId, content);
        String json = objectMapper.writeValueAsString(response);

        // Then - JSON 필드 검증
        assertThat(json).isNotNull();
        assertThat(json).contains("messageId");
        assertThat(json).contains("roomId");
        assertThat(json).contains("content");
        assertThat(json).contains("senderName");

        // 역직렬화 검증
        ChatMessageSendResponse deserialized = objectMapper.readValue(json, ChatMessageSendResponse.class);
        assertThat(deserialized.content()).isEqualTo(response.content());
        assertThat(deserialized.senderName()).isEqualTo("맹구씨");

        System.out.println("직렬화된 JSON: " + json);
    }
}
