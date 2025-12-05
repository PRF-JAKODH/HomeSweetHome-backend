package com.homesweet.homesweetback.domain.chat.service.Imp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.chat.dto.ChatMessageDto;
import com.homesweet.homesweetback.domain.chat.dto.response.ChatMessageSendResponse;
import com.homesweet.homesweetback.domain.chat.dto.response.PreMessageResponse;
import com.homesweet.homesweetback.domain.chat.entity.ChatMessage;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatUserRole;
import com.homesweet.homesweetback.domain.chat.entity.enums.MessageType;
import com.homesweet.homesweetback.domain.chat.repository.jpa.ChatMessageRepository;
import com.homesweet.homesweetback.domain.chat.repository.jpa.ChatRoomRepository;
import com.homesweet.homesweetback.domain.chat.repository.jpa.RoomMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * @author hygg0408e@gmail.com
 * @date 25. 11. 11.
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("[Service] 채팅 메시지 서비스 단위 테스트")
class ChatMessageServiceUnitTest {

    @InjectMocks
    private ChatMessageServiceImpl chatMessageService;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long userId;
    private Long roomId;
    private String content;
    private User mockUser;
    private ChatRoom mockRoom;
    private RoomMember mockMember;
    private ChatMessage mockMessage;

    // 실제 엔티티 객체 (JSON 직렬화 테스트용)
    private User testUser;
    private ChatRoom testRoom;
    private RoomMember roomMember;
    private ChatMessage chatMessage;

    @BeforeEach
    void setUp() {
        // ObjectMapper 설정
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.registerModule(new Jdk8Module());

        // ===== 테스트 데이터 초기화 =====
        userId = 1L;
        roomId = 100L;
        content = "테스트 메시지";

        // ===== Mock 객체 생성 =====
        mockUser = mock(User.class);
        mockRoom = mock(ChatRoom.class);
        mockMember = mock(RoomMember.class);
        mockMessage = mock(ChatMessage.class);

        // 실제 엔티티 객체 초기화 (JSON 직렬화 테스트용)
        testUser = User.builder()
                .id(1L)
                .name("맹구씨")
                .profileImageUrl("https://test.com/profile.jpg")
                .build();

        testRoom = ChatRoom.builder()
                .id(100L)
                .build();

        roomMember = RoomMember.createMember(testRoom, testUser, ChatUserRole.MEMBER);

        chatMessage = ChatMessage.builder()
                .id(1L)
                .room(testRoom)
                .sender(testUser)
                .content("안녕 짱구씨")
                .messageType(MessageType.TEXT)
                .sentAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("메시지 전송 테스트")
    class SendMessageTest {

        @Test
        @DisplayName("[성공] 메시지를 정상적으로 전송한다")
        void sendMessage_success() {
            // Given
            LocalDateTime sentAt = LocalDateTime.now();

            // [1] 채팅방 조회
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(mockRoom));

            // [2] 발신자 조회
            given(roomMemberRepository.findByUserIdAndRoomId(userId, roomId)).willReturn(mockMember);

            // [3] 퇴장 여부 확인
            given(mockMember.isExit()).willReturn(false);

            // [4] User 정보 가져오기
            given(mockMember.getUser()).willReturn(mockUser);
            given(mockUser.getName()).willReturn("맹구");
            given(mockUser.getProfileImageUrl()).willReturn("profile.png");

            // [5] 메시지 저장
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockMessage);

            // DTO.from()에서 사용하는 것들
            given(mockMessage.getId()).willReturn(100L);
            given(mockMessage.getContent()).willReturn(content);
            given(mockMessage.getSentAt()).willReturn(sentAt);
            given(mockMessage.getMessageType()).willReturn(MessageType.TEXT);

            // When
            ChatMessageSendResponse response = chatMessageService.sendMessage(roomId, userId, content);

            // Then
            // ===== 1. Return 값 검증  =====
            assertThat(response).isNotNull();
            assertThat(response.messageId()).isEqualTo(100L);
            assertThat(response.content()).isEqualTo(content);
            assertThat(response.roomId()).isEqualTo(roomId);
            assertThat(response.senderId()).isEqualTo(userId);
            assertThat(response.senderName()).isEqualTo("맹구");
            assertThat(response.senderProfileImg()).isEqualTo("profile.png");
            assertThat(response.messageType()).isEqualTo(MessageType.TEXT);
            assertThat(response.sentAt()).isEqualTo(sentAt);
            assertThat(response.isRead()).isTrue();

            // ===== 2. 메서드 호출 검증 =====
            verify(chatRoomRepository).findById(roomId);
            verify(roomMemberRepository).findByUserIdAndRoomId(userId, roomId);
            verify(chatMessageRepository).save(any(ChatMessage.class));
            verify(mockRoom).updateLastMessage(content, sentAt);
        }

        @Test
        @DisplayName("[성공] 채팅방 마지막 메시지가 업데이트된다")
        void sendMessage_shouldUpdateChatRoomLastMessage() {
            // Given
            LocalDateTime sentAt = LocalDateTime.now();

            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(mockRoom));
            given(roomMemberRepository.findByUserIdAndRoomId(userId, roomId)).willReturn(mockMember);
            given(mockMember.isExit()).willReturn(false);
            given(mockMember.getUser()).willReturn(mockUser);
            given(mockUser.getName()).willReturn("맹구");
            given(mockUser.getProfileImageUrl()).willReturn("profile.png");
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockMessage);
            given(mockMessage.getId()).willReturn(100L);
            given(mockMessage.getContent()).willReturn(content);
            given(mockMessage.getSentAt()).willReturn(sentAt);
            given(mockMessage.getMessageType()).willReturn(MessageType.TEXT);

            // When
            chatMessageService.sendMessage(roomId, userId, content);

            // Then
            verify(mockRoom).updateLastMessage(content, sentAt);
        }

        @Test
        @DisplayName("[성공] 메시지 타입이 TEXT로 저장된다")
        void sendMessage_shouldSaveWithTextMessageType() {
            // Given
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(mockRoom));
            given(roomMemberRepository.findByUserIdAndRoomId(userId, roomId)).willReturn(mockMember);
            given(mockMember.isExit()).willReturn(false);
            given(mockMember.getUser()).willReturn(mockUser);
            given(mockUser.getName()).willReturn("맹구");
            given(mockUser.getProfileImageUrl()).willReturn("profile.png");
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockMessage);
            given(mockMessage.getId()).willReturn(100L);
            given(mockMessage.getContent()).willReturn(content);
            given(mockMessage.getSentAt()).willReturn(LocalDateTime.now());
            given(mockMessage.getMessageType()).willReturn(MessageType.TEXT);


            // When
            ChatMessageSendResponse response = chatMessageService.sendMessage(roomId, userId, content);

            // Then
            assertThat(response.messageType()).isEqualTo(MessageType.TEXT);
            verify(chatMessageRepository).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 채팅방이면 예외가 발생한다")
        void sendMessage_roomNotFound_throwsException() {
            // Given
            Long invalidRoomId = 999L;
            given(chatRoomRepository.findById(invalidRoomId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    chatMessageService.sendMessage(invalidRoomId, userId, content)
            )
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ROOM_NOT_FOUND);


            verify(chatRoomRepository).findById(invalidRoomId);
            verify(roomMemberRepository, never()).findByUserIdAndRoomId(anyLong(), anyLong());
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("[실패] 채팅방 멤버가 아니면 예외가 발생한다")
        void sendMessage_notMember_throwsException() {
            // Given
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(mockRoom));
            given(roomMemberRepository.findByUserIdAndRoomId(userId, roomId)).willReturn(null);

            // When & Then
            assertThatThrownBy(() ->
                    chatMessageService.sendMessage(roomId, userId, content)
            )
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ROOM_MEMBER_NOT_FOUND);

            verify(chatRoomRepository).findById(roomId);
            verify(roomMemberRepository).findByUserIdAndRoomId(userId, roomId);
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("[실패] 이미 퇴장한 사용자이면 예외가 발생한다")
        void sendMessage_exitedMember_throwsException() {
            // Given
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(mockRoom));
            given(roomMemberRepository.findByUserIdAndRoomId(userId, roomId)).willReturn(mockMember);
            given(mockMember.isExit()).willReturn(true);

            // When & Then
            assertThatThrownBy(() ->
                    chatMessageService.sendMessage(roomId, userId, content)
            )
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ROOM_MEMBER_NOT_FOUND);

            verify(chatRoomRepository).findById(roomId);
            verify(roomMemberRepository).findByUserIdAndRoomId(userId, roomId);
            verify(mockMember).isExit();
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("[검증] 메시지 저장 시 save 메서드가 호출된다")
        void sendMessage_shouldCallSaveMethod() {
            // Given
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(mockRoom));
            given(roomMemberRepository.findByUserIdAndRoomId(userId, roomId)).willReturn(mockMember);
            given(mockMember.isExit()).willReturn(false);
            given(mockMember.getUser()).willReturn(mockUser);
            given(mockUser.getName()).willReturn("맹구");
            given(mockUser.getProfileImageUrl()).willReturn("profile.png");
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockMessage);
            given(mockMessage.getId()).willReturn(100L);
            given(mockMessage.getContent()).willReturn(content);
            given(mockMessage.getSentAt()).willReturn(LocalDateTime.now());
            given(mockMessage.getMessageType()).willReturn(MessageType.TEXT);

            // When
            chatMessageService.sendMessage(roomId, userId, content);

            // Then
            verify(chatMessageRepository).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("[검증] 응답의 isRead는 자신이 보낸 메시지이므로 true이다")
        void sendMessage_isReadShouldBeTrue() {
            // Given
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(mockRoom));
            given(roomMemberRepository.findByUserIdAndRoomId(userId, roomId)).willReturn(mockMember);
            given(mockMember.isExit()).willReturn(false);
            given(mockMember.getUser()).willReturn(mockUser);
            given(mockUser.getName()).willReturn("맹구");
            given(mockUser.getProfileImageUrl()).willReturn("profile.png");
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockMessage);
            given(mockMessage.getId()).willReturn(100L);
            given(mockMessage.getContent()).willReturn(content);
            given(mockMessage.getSentAt()).willReturn(LocalDateTime.now());
            given(mockMessage.getMessageType()).willReturn(MessageType.TEXT);

            // When
            ChatMessageSendResponse response = chatMessageService.sendMessage(roomId, userId, content);

            // Then
            assertThat(response.isRead()).isTrue();
        }
    }

    @Nested
    @DisplayName("WebSocket JSON 직렬화 테스트")
    class JsonSerializationTest {

        @Test
        @DisplayName("[성공] WebSocket으로 전송되는 응답 DTO를 JSON으로 직렬화한다")
        void sendMessage_JsonSerialization() throws Exception {
            // Given
            Long roomId = 100L;
            Long senderId = 1L;
            String content = "안녕 짱구씨";

            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
            given(roomMemberRepository.findByUserIdAndRoomId(senderId, roomId)).willReturn(roomMember);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(chatMessage);

            // When
            ChatMessageSendResponse response = chatMessageService.sendMessage(roomId, senderId, content);
            String json = objectMapper.writeValueAsString(response);

            // Then - JSON 필드 검증
            assertThat(json).isNotNull();
            assertThat(json).contains("messageId");
            assertThat(json).contains("roomId");
            assertThat(json).contains("content");
            assertThat(json).contains("senderName");
            assertThat(json).contains("senderProfileImg");
            assertThat(json).contains("messageType");
            assertThat(json).contains("sentAt");
            assertThat(json).contains("isRead");

            System.out.println(" 직렬화된 JSON: " + json);
        }

        @Test
        @DisplayName("[성공] JSON 역직렬화가 정상적으로 동작한다")
        void sendMessage_JsonDeserialization() throws Exception {
            // Given
            Long roomId = 100L;
            Long senderId = 1L;
            String content = "안녕 짱구씨";

            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
            given(roomMemberRepository.findByUserIdAndRoomId(senderId, roomId)).willReturn(roomMember);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(chatMessage);

            // When
            ChatMessageSendResponse response = chatMessageService.sendMessage(roomId, senderId, content);
            String json = objectMapper.writeValueAsString(response);
            ChatMessageSendResponse deserialized = objectMapper.readValue(json, ChatMessageSendResponse.class);

            // Then
            assertThat(deserialized).isNotNull();
            assertThat(deserialized.content()).isEqualTo(response.content());
            assertThat(deserialized.senderName()).isEqualTo("맹구씨");
            assertThat(deserialized.messageId()).isEqualTo(response.messageId());
            assertThat(deserialized.roomId()).isEqualTo(response.roomId());
            assertThat(deserialized.senderId()).isEqualTo(response.senderId());
            assertThat(deserialized.messageType()).isEqualTo(response.messageType());
            assertThat(deserialized.isRead()).isEqualTo(response.isRead());

        }

        @Test
        @DisplayName("[성공] LocalDateTime이 ISO-8601 형식으로 직렬화된다")
        void sendMessage_LocalDateTimeSerialization() throws Exception {
            // Given
            Long roomId = 100L;
            Long senderId = 1L;
            String content = "시간 테스트";

            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
            given(roomMemberRepository.findByUserIdAndRoomId(senderId, roomId)).willReturn(roomMember);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(chatMessage);

            // When
            ChatMessageSendResponse response = chatMessageService.sendMessage(roomId, senderId, content);
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertThat(json).contains("sentAt");
            // ISO-8601 형식 확인 (예: "2024-11-18T12:34:56")
            assertThat(json).containsPattern("\"sentAt\":\\s*\"\\d{4}-\\d{2}-\\d{2}T");

            System.out.println("sentAt 직렬화 형식: " + json);
        }

        @Test
        @DisplayName("[성공] 모든 필드가 올바른 타입으로 직렬화된다")
        void sendMessage_AllFieldsSerialization() throws Exception {
            // Given
            Long roomId = 100L;
            Long senderId = 1L;
            String content = "전체 필드 테스트";

            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
            given(roomMemberRepository.findByUserIdAndRoomId(senderId, roomId)).willReturn(roomMember);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(chatMessage);

            // When
            ChatMessageSendResponse response = chatMessageService.sendMessage(roomId, senderId, content);
            String json = objectMapper.writeValueAsString(response);

            // Then - 각 필드 타입 검증
            assertThat(json).containsPattern("\"messageId\":\\s*\\d+");          // Long
            assertThat(json).containsPattern("\"roomId\":\\s*\\d+");             // Long
            assertThat(json).containsPattern("\"senderId\":\\s*\\d+");           // Long
            assertThat(json).containsPattern("\"senderName\":\\s*\"[^\"]+\"");   // String
            assertThat(json).containsPattern("\"content\":\\s*\"[^\"]+\"");      // String
            assertThat(json).containsPattern("\"messageType\":\\s*\"TEXT\"");    // Enum
            assertThat(json).containsPattern("\"isRead\":\\s*(true|false)");     // Boolean

            System.out.println("전체 필드 직렬화 검증 완료");
        }
    }

    @Nested
    @DisplayName("이전 메세지 조회 테스트")
    class GetPreMessageTest {

        @Test
        @DisplayName("[성공] 최초 로드 시 메시지를 조회한다")
        void getPreMessage_InitialLoad_Success() {
            // given
            int size = 20;
            Pageable pageable = PageRequest.of(0, size);
            LocalDateTime baseTime = LocalDateTime.now();

            // 실제 Entity 사용 (기존 setUp의 testUser, testRoom 활용)
            List<ChatMessage> messages = Arrays.asList(
                    ChatMessage.builder()
                            .id(3L)
                            .room(testRoom)
                            .sender(testUser)
                            .content("테스트 메시지 3")
                            .messageType(MessageType.TEXT)
                            .sentAt(baseTime)
                            .build(),
                    ChatMessage.builder()
                            .id(2L)
                            .room(testRoom)
                            .sender(testUser)
                            .content("테스트 메시지 2")
                            .messageType(MessageType.TEXT)
                            .sentAt(baseTime.minusMinutes(1))
                            .build(),
                    ChatMessage.builder()
                            .id(1L)
                            .room(testRoom)
                            .sender(testUser)
                            .content("테스트 메시지 1")
                            .messageType(MessageType.TEXT)
                            .sentAt(baseTime.minusMinutes(2))
                            .build()
            );

            Slice<ChatMessage> slice = new SliceImpl<>(messages, pageable, true);

            when(chatMessageRepository.findByRoom_IdOrderBySentAtDesc(eq(roomId), any(Pageable.class)))
                    .thenReturn(slice);

            // when
            PreMessageResponse response = chatMessageService.getPreMessage(roomId, null, size);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getMessages()).hasSize(3);
            assertThat(response.isHasMore()).isTrue();

            // 메시지 순서 검증 (최신 순)
            assertThat(response.getMessages().get(0).content()).isEqualTo("테스트 메시지 3");
            assertThat(response.getMessages().get(1).content()).isEqualTo("테스트 메시지 2");
            assertThat(response.getMessages().get(2).content()).isEqualTo("테스트 메시지 1");

            // 사용자 정보 검증
            assertThat(response.getMessages().get(0).senderName()).isEqualTo("맹구씨");
            assertThat(response.getMessages().get(0).profileImageUrl())
                    .isEqualTo("https://test.com/profile.jpg");

            // 채팅방 ID 검증
            assertThat(response.getMessages().get(0).roomId()).isEqualTo(roomId);

            verify(chatMessageRepository, times(1))
                    .findByRoom_IdOrderBySentAtDesc(eq(roomId), any(Pageable.class));
            verify(chatMessageRepository, never())
                    .findOlderMessages(anyLong(), anyLong(), any(Pageable.class));
        }

        @Test
        @DisplayName("[성공] 추가 로드 시 이전 메시지를 조회한다")
        void getPreMessage_AdditionalLoad_Success() {
            // given
            Long lastMessageId = 3L;
            int size = 20;
            Pageable pageable = PageRequest.of(0, size);
            LocalDateTime baseTime = LocalDateTime.now();

            List<ChatMessage> messages = Arrays.asList(
                    ChatMessage.builder()
                            .id(2L)
                            .room(testRoom)
                            .sender(testUser)
                            .content("테스트 메시지 2")
                            .messageType(MessageType.TEXT)
                            .sentAt(baseTime)
                            .build(),
                    ChatMessage.builder()
                            .id(1L)
                            .room(testRoom)
                            .sender(testUser)
                            .content("테스트 메시지 1")
                            .messageType(MessageType.TEXT)
                            .sentAt(baseTime.minusMinutes(1))
                            .build()
            );

            Slice<ChatMessage> slice = new SliceImpl<>(messages, pageable, false);

            when(chatMessageRepository.findOlderMessages(eq(roomId), eq(lastMessageId), any(Pageable.class)))
                    .thenReturn(slice);

            // when
            PreMessageResponse response = chatMessageService.getPreMessage(roomId, lastMessageId, size);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getMessages()).hasSize(2);
            assertThat(response.isHasMore()).isFalse();
            assertThat(response.getMessages().get(0).content()).isEqualTo("테스트 메시지 2");
            assertThat(response.getMessages().get(1).content()).isEqualTo("테스트 메시지 1");

            verify(chatMessageRepository, times(1))
                    .findOlderMessages(eq(roomId), eq(lastMessageId), any(Pageable.class));
            verify(chatMessageRepository, never())
                    .findByRoom_IdOrderBySentAtDesc(anyLong(), any(Pageable.class));
        }

        @Test
        @DisplayName("[성공] 메시지가 없는 경우 빈 리스트를 반환한다")
        void getPreMessage_NoMessages_ReturnsEmptyList() {
            // given
            int size = 20;
            Pageable pageable = PageRequest.of(0, size);
            Slice<ChatMessage> emptySlice = new SliceImpl<>(Collections.emptyList(), pageable, false);

            when(chatMessageRepository.findByRoom_IdOrderBySentAtDesc(eq(roomId), any(Pageable.class)))
                    .thenReturn(emptySlice);

            // when
            PreMessageResponse response = chatMessageService.getPreMessage(roomId, null, size);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getMessages()).isEmpty();
            assertThat(response.isHasMore()).isFalse();
        }

        @Test
        @DisplayName("[성공] 다음 페이지가 없는 경우 hasMore가 false이다")
        void getPreMessage_NoMorePages_HasMoreIsFalse() {
            // given
            int size = 20;
            Pageable pageable = PageRequest.of(0, size);
            LocalDateTime baseTime = LocalDateTime.now();

            List<ChatMessage> messages = Arrays.asList(
                    ChatMessage.builder()
                            .id(1L)
                            .room(testRoom)
                            .sender(testUser)
                            .content("마지막 메시지")
                            .messageType(MessageType.TEXT)
                            .sentAt(baseTime)
                            .build()
            );

            Slice<ChatMessage> slice = new SliceImpl<>(messages, pageable, false);

            when(chatMessageRepository.findByRoom_IdOrderBySentAtDesc(eq(roomId), any(Pageable.class)))
                    .thenReturn(slice);

            // when
            PreMessageResponse response = chatMessageService.getPreMessage(roomId, null, size);

            // then
            assertThat(response.isHasMore()).isFalse();
        }

        @Test
        @DisplayName("[성공] 요청한 size만큼만 메시지를 조회한다")
        void getPreMessage_RespectsPageSize() {
            // given
            int size = 2;
            Pageable pageable = PageRequest.of(0, size);
            LocalDateTime baseTime = LocalDateTime.now();

            List<ChatMessage> messages = Arrays.asList(
                    ChatMessage.builder()
                            .id(2L)
                            .room(testRoom)
                            .sender(testUser)
                            .content("테스트 메시지 2")
                            .messageType(MessageType.TEXT)
                            .sentAt(baseTime)
                            .build(),
                    ChatMessage.builder()
                            .id(1L)
                            .room(testRoom)
                            .sender(testUser)
                            .content("테스트 메시지 1")
                            .messageType(MessageType.TEXT)
                            .sentAt(baseTime.minusMinutes(1))
                            .build()
            );

            Slice<ChatMessage> slice = new SliceImpl<>(messages, pageable, true);

            when(chatMessageRepository.findByRoom_IdOrderBySentAtDesc(eq(roomId), any(Pageable.class)))
                    .thenReturn(slice);

            // when
            PreMessageResponse response = chatMessageService.getPreMessage(roomId, null, size);

            // then
            assertThat(response.getMessages()).hasSize(2);
            assertThat(response.isHasMore()).isTrue();
        }

        @Test
        @DisplayName("[검증] lastMessageId가 null일 때 초기 로드 메서드를 호출한다")
        void getPreMessage_WhenLastMessageIdIsNull_CallsInitialLoadMethod() {
            // given
            int size = 10;
            LocalDateTime baseTime = LocalDateTime.now();

            List<ChatMessage> messages = Arrays.asList(
                    ChatMessage.builder()
                            .id(1L)
                            .room(testRoom)
                            .sender(testUser)
                            .content("테스트 메시지")
                            .messageType(MessageType.TEXT)
                            .sentAt(baseTime)
                            .build()
            );

            Slice<ChatMessage> slice = new SliceImpl<>(messages, PageRequest.of(0, size), false);

            when(chatMessageRepository.findByRoom_IdOrderBySentAtDesc(eq(roomId), any(Pageable.class)))
                    .thenReturn(slice);

            // when
            chatMessageService.getPreMessage(roomId, null, size);

            // then
            verify(chatMessageRepository, times(1))
                    .findByRoom_IdOrderBySentAtDesc(eq(roomId), any(Pageable.class));
            verify(chatMessageRepository, never())
                    .findOlderMessages(anyLong(), anyLong(), any(Pageable.class));
        }

        @Test
        @DisplayName("[검증] lastMessageId가 있을 때 추가 로드 메서드를 호출한다")
        void getPreMessage_WhenLastMessageIdExists_CallsAdditionalLoadMethod() {
            // given
            Long lastMessageId = 10L;
            int size = 10;
            LocalDateTime baseTime = LocalDateTime.now();

            List<ChatMessage> messages = Arrays.asList(
                    ChatMessage.builder()
                            .id(1L)
                            .room(testRoom)
                            .sender(testUser)
                            .content("테스트 메시지")
                            .messageType(MessageType.TEXT)
                            .sentAt(baseTime)
                            .build()
            );

            Slice<ChatMessage> slice = new SliceImpl<>(messages, PageRequest.of(0, size), false);

            when(chatMessageRepository.findOlderMessages(eq(roomId), eq(lastMessageId), any(Pageable.class)))
                    .thenReturn(slice);

            // when
            chatMessageService.getPreMessage(roomId, lastMessageId, size);

            // then
            verify(chatMessageRepository, times(1))
                    .findOlderMessages(eq(roomId), eq(lastMessageId), any(Pageable.class));
            verify(chatMessageRepository, never())
                    .findByRoom_IdOrderBySentAtDesc(anyLong(), any(Pageable.class));
        }

        @Test
        @DisplayName("[검증] DTO 변환 시 사용자 정보가 정확히 매핑된다")
        void getPreMessage_UserInfoMappedCorrectly() {
            // given
            int size = 10;
            LocalDateTime baseTime = LocalDateTime.now();

            List<ChatMessage> messages = Arrays.asList(
                    ChatMessage.builder()
                            .id(1L)
                            .room(testRoom)
                            .sender(testUser)
                            .content("테스트 메시지 1")
                            .messageType(MessageType.TEXT)
                            .sentAt(baseTime)
                            .build(),
                    ChatMessage.builder()
                            .id(2L)
                            .room(testRoom)
                            .sender(testUser)
                            .content("테스트 메시지 2")
                            .messageType(MessageType.TEXT)
                            .sentAt(baseTime.minusMinutes(1))
                            .build()
            );

            Slice<ChatMessage> slice = new SliceImpl<>(messages, PageRequest.of(0, size), false);

            when(chatMessageRepository.findByRoom_IdOrderBySentAtDesc(eq(roomId), any(Pageable.class)))
                    .thenReturn(slice);

            // when
            PreMessageResponse response = chatMessageService.getPreMessage(roomId, null, size);

            // then
            response.getMessages().forEach(message -> {
                assertThat(message.senderName()).isEqualTo("맹구씨");
                assertThat(message.profileImageUrl())
                        .isEqualTo("https://test.com/profile.jpg");
            });
        }

        @Test
        @DisplayName("[검증] DTO 변환 시 모든 필드가 정확히 매핑된다")
        void getPreMessage_AllFieldsMappedCorrectly() {
            // given
            int size = 10;
            LocalDateTime sentAt = LocalDateTime.of(2024, 11, 19, 15, 30);

            List<ChatMessage> messages = Arrays.asList(
                    ChatMessage.builder()
                            .id(100L)
                            .room(testRoom)
                            .sender(testUser)
                            .content("전체 필드 테스트")
                            .messageType(MessageType.TEXT)
                            .sentAt(sentAt)
                            .build()
            );

            Slice<ChatMessage> slice = new SliceImpl<>(messages, PageRequest.of(0, size), false);

            when(chatMessageRepository.findByRoom_IdOrderBySentAtDesc(eq(roomId), any(Pageable.class)))
                    .thenReturn(slice);

            // when
            PreMessageResponse response = chatMessageService.getPreMessage(roomId, null, size);

            // then
            ChatMessageDto dto = response.getMessages().get(0);
            assertThat(dto.messageId()).isEqualTo(100L);
            assertThat(dto.roomId()).isEqualTo(roomId);
            assertThat(dto.senderId()).isEqualTo(testUser.getId());
            assertThat(dto.content()).isEqualTo("전체 필드 테스트");
            assertThat(dto.sentAt()).isEqualTo(sentAt);
            assertThat(dto.senderName()).isEqualTo("맹구씨");
            assertThat(dto.profileImageUrl()).isEqualTo("https://test.com/profile.jpg");
        }
    }
}