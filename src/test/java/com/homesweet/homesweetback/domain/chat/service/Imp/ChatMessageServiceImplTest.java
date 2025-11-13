//package com.homesweet.homesweetback.domain.chat.service.Imp;
//
//import com.homesweet.homesweetback.domain.auth.entity.User;
//import com.homesweet.homesweetback.domain.chat.dto.response.ChatMessageSendResponse;
//import com.homesweet.homesweetback.domain.chat.entity.ChatMessage;
//import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
//import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
//import com.homesweet.homesweetback.domain.chat.entity.enums.MessageType;
//import com.homesweet.homesweetback.domain.chat.repository.ChatMessageRepository;
//import com.homesweet.homesweetback.domain.chat.repository.ChatRoomRepository;
//import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
//import com.homesweet.homesweetback.domain.chat.service.ChatMessageService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.util.Optional;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//
///**
// *
// * @author hygg0408e@gmail.com
// * @date 25. 11. 11.
// */
//@ActiveProfiles("test")
//@ExtendWith(MockitoExtension.class)
//class ChatMessageServiceImplTest {
//
//    @Mock private ChatRoomRepository chatRoomRepository;
//    @Mock private ChatMessageRepository chatMessageRepository;
//    @Mock private RoomMemberRepository roomMemberRepository;
//
//    @InjectMocks private ChatMessageService chatMessageService;
//
//    @DisplayName("[Unit] 정상 메시지 전송 - sendMessage() 성공")
//    @Test
//    void sendMessage_shouldSaveMessageSuccessfully() {
//        // Given
//        Long roomId = 1L;
//        Long senderId = 10L;
//        String content = "안녕";
//
//        ChatRoom room = ChatRoom.builder().id(roomId).build();
//        User user = User.builder().id(senderId).name("주아현").profileImageUrl("test.png").build();
//        RoomMember member = RoomMember.builder().user(user).isExit(false).build();
//        ChatMessage savedMessage = ChatMessage.builder()
//                .room(room)
//                .sender(user)
//                .messageType(MessageType.TEXT)
//                .content(content)
//                .build();
//
//        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
//        when(roomMemberRepository.findByUserIdAndRoomId(senderId, roomId)).thenReturn(member);
//        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);
//
//        // When
//        ChatMessageSendResponse response = chatMessageService.sendMessage(roomId, senderId, content);
//
//        // Then
//        assertNotNull(response);
//        assertEquals(roomId, response.roomId());
//        assertEquals(senderId, response.senderId());
//        assertEquals("안녕", response.content());
//
//        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
//        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
//    }
//
//    @DisplayName("[Unit] 존재하지 않는 채팅방 - IllegalArgumentException 발생")
//    @Test
//    void sendMessage_shouldThrowException_whenRoomNotFound() {
//        // Given
//        Long roomId = 999L;
//        Long senderId = 10L;
//        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.empty());
//
//        // When & Then
//        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
//                () -> chatMessageService.sendMessage(roomId, senderId, "테스트"));
//        assertEquals("존재하지 않는 채팅방입니다.", ex.getMessage());
//    }
//}
