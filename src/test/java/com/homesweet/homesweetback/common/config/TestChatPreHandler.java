//package com.homesweet.homesweetback.common.config;
//
//import com.homesweet.homesweetback.common.exception.BusinessException;
//import com.homesweet.homesweetback.common.exception.ErrorCode;
//import com.homesweet.homesweetback.common.security.jwt.JwtTokenProvider;
//import com.homesweet.homesweetback.domain.chat.service.ChatMessageService;
//import com.homesweet.homesweetback.domain.chat.service.ChatRoomService;
//import io.jsonwebtoken.Claims;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.MessageChannel;
//import org.springframework.messaging.simp.stomp.StompCommand;
//import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
//import org.springframework.messaging.support.MessageBuilder;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.util.HashMap;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.BDDMockito.*;
//
//@ActiveProfiles("test")
//@ExtendWith(MockitoExtension.class)
//@DisplayName("[Interceptor] ChatPreHandler 단위 테스트")
//class TestChatPreHandler {
//
//    @InjectMocks
//    private com.homesweet.homesweetback.common.config.interceptor.ChatPreHandler chatPreHandler;
//
//    @Mock
//    private JwtTokenProvider jwtTokenProvider;
//
//    @Mock
//    private ChatRoomService chatRoomService;
//
//    @Mock
//    private ChatMessageService chatMessageService;
//
//    @Mock
//    private MessageChannel messageChannel;
//
//    @Mock
//    private Claims claims;
//
//    /**
//     * Message를 mutable 상태로 생성
//     */
//    private Message<byte[]> createMessage(StompHeaderAccessor accessor) {
//        accessor.setLeaveMutable(true);
//        return MessageBuilder
//                .createMessage(new byte[0], accessor.getMessageHeaders());
//    }
//
//    @Nested
//    @DisplayName("CONNECT 커맨드 테스트")
//    class ConnectCommandTest {
//        private final String VALID_TOKEN = "valid.jwt.token";
//        private final Long USER_ID = 123L;
//        private final String USER_ID_STR = String.valueOf(USER_ID);
//
//        @Test
//        @DisplayName("[성공] JWT 토큰 검증 성공 및 userId 저장")
//        void handleConnect_Success() {
//            // Given
//            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
//            accessor.setSessionAttributes(new HashMap<>());
//            accessor.setNativeHeader("Authorization", "Bearer " + VALID_TOKEN);
//
//            given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(true);
//            given(jwtTokenProvider.isRefreshToken(VALID_TOKEN)).willReturn(false);
//            given(jwtTokenProvider.getClaimsFromToken(VALID_TOKEN)).willReturn(claims);
//            given(claims.getSubject()).willReturn(USER_ID_STR);
//            given(claims.get("role", String.class)).willReturn("USER");
//
//            Message<byte[]> message = createMessage(accessor);
//
//            // When
//            Message<?> result = chatPreHandler.preSend(message, messageChannel);
//
//            // Then
//            assertThat(result).isNotNull();
//
//            StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
//            assertThat(resultAccessor.getSessionAttributes().get("userId")).isEqualTo(USER_ID);
//
//            then(jwtTokenProvider).should(times(1)).validateToken(VALID_TOKEN);
//            then(jwtTokenProvider).should(times(1)).isRefreshToken(VALID_TOKEN);
//            then(jwtTokenProvider).should(times(1)).getClaimsFromToken(VALID_TOKEN);
//        }
//
//        @Test
//        @DisplayName("[실패] JWT 토큰이 없으면 예외 발생")
//        void handleConnect_NoToken() {
//            // Given
//            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
//            accessor.setSessionAttributes(new HashMap<>());
//
//            Message<byte[]> message = createMessage(accessor);
//
//            // When & Then
//            assertThatThrownBy(() -> chatPreHandler.preSend(message, messageChannel))
//                    .isInstanceOf(BusinessException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_MISSING);
//
//            then(jwtTokenProvider).should(never()).validateToken(any());
//        }
//
//        @Test
//        @DisplayName("[실패] 유효하지 않은 토큰이면 예외 발생")
//        void handleConnect_InvalidToken() {
//            // Given
//            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
//            accessor.setSessionAttributes(new HashMap<>());
//            accessor.setNativeHeader("Authorization", "Bearer " + VALID_TOKEN);
//
//            given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(false);
//
//            Message<byte[]> message = createMessage(accessor);
//
//            // When & Then
//            assertThatThrownBy(() -> chatPreHandler.preSend(message, messageChannel))
//                    .isInstanceOf(BusinessException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_INVALID);
//
//            then(jwtTokenProvider).should(times(1)).validateToken(VALID_TOKEN);
//        }
//
//        @Test
//        @DisplayName("[실패] Refresh 토큰으로 연결 시도하면 예외 발생")
//        void handleConnect_RefreshToken() {
//            // Given
//            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
//            accessor.setSessionAttributes(new HashMap<>());
//            accessor.setNativeHeader("Authorization", "Bearer " + VALID_TOKEN);
//
//            given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(true);
//            given(jwtTokenProvider.isRefreshToken(VALID_TOKEN)).willReturn(true);
//
//            Message<byte[]> message = createMessage(accessor);
//
//            // When & Then
//            assertThatThrownBy(() -> chatPreHandler.preSend(message, messageChannel))
//                    .isInstanceOf(BusinessException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_REFRESH_NOT_ALLOWED);
//
//            then(jwtTokenProvider).should(times(1)).validateToken(VALID_TOKEN);
//            then(jwtTokenProvider).should(times(1)).isRefreshToken(VALID_TOKEN);
//        }
//    }
//
//    @Nested
//    @DisplayName("SUBSCRIBE 커맨드 테스트")
//    class SubscribeCommandTest {
//        private final Long USER_ID = 123L;
//        private final Long ROOM_ID = 100L;
//
//        @Test
//        @DisplayName("[성공] 방 멤버이면 구독 성공")
//        void handleSubscribe_Success() {
//            // Given
//            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
//            accessor.setSessionAttributes(new HashMap<>());
//            accessor.setDestination("/sub/rooms/" + ROOM_ID);
//            accessor.getSessionAttributes().put("userId", USER_ID);
//
//            given(chatRoomService.isUserInRoom(USER_ID, ROOM_ID)).willReturn(true);
//
//            Message<byte[]> message = createMessage(accessor);
//
//            // When
//            Message<?> result = chatPreHandler.preSend(message, messageChannel);
//
//            // Then
//            assertThat(result).isNotNull();
//            then(chatRoomService).should(times(1)).isUserInRoom(USER_ID, ROOM_ID);
//        }
//
//        @Test
//        @DisplayName("[실패] 방 멤버가 아니면 구독 실패")
//        void handleSubscribe_NotMember() {
//            // Given
//            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
//            accessor.setSessionAttributes(new HashMap<>());
//            accessor.setDestination("/sub/rooms/" + ROOM_ID);
//            accessor.getSessionAttributes().put("userId", USER_ID);
//
//            given(chatRoomService.isUserInRoom(USER_ID, ROOM_ID)).willReturn(false);
//
//            Message<byte[]> message = createMessage(accessor);
//
//            // When & Then
//            assertThatThrownBy(() -> chatPreHandler.preSend(message, messageChannel))
//                    .isInstanceOf(BusinessException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_UNAUTHORIZED_ACCESS);
//
//            then(chatRoomService).should(times(1)).isUserInRoom(USER_ID, ROOM_ID);
//        }
//    }
//
//    @Nested
//    @DisplayName("SEND 커맨드 테스트")
//    class SendCommandTest {
//        private final Long USER_ID = 123L;
//        private final Long ROOM_ID = 100L;
//
//        @Test
//        @DisplayName("[성공] 메시지 전송 권한이 있으면 성공")
//        void handleSend_Success() {
//            // Given
//            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
//            accessor.setSessionAttributes(new HashMap<>());
//            accessor.setDestination("/pub/chat.send/" + ROOM_ID);
//            accessor.getSessionAttributes().put("userId", USER_ID);
//
//            given(chatMessageService.canSendMessage(USER_ID, ROOM_ID)).willReturn(true);
//
//            Message<byte[]> message = createMessage(accessor);
//
//            // When
//            Message<?> result = chatPreHandler.preSend(message, messageChannel);
//
//            // Then
//            assertThat(result).isNotNull();
//            then(chatMessageService).should(times(1)).canSendMessage(USER_ID, ROOM_ID);
//        }
//
//        @Test
//        @DisplayName("[실패] 메시지 전송 권한이 없으면 실패")
//        void handleSend_NoPermission() {
//            // Given
//            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
//            accessor.setSessionAttributes(new HashMap<>());
//            accessor.setDestination("/pub/chat.send/" + ROOM_ID);
//            accessor.getSessionAttributes().put("userId", USER_ID);
//
//            given(chatMessageService.canSendMessage(USER_ID, ROOM_ID)).willReturn(false);
//
//            Message<byte[]> message = createMessage(accessor);
//
//            // When & Then
//            assertThatThrownBy(() -> chatPreHandler.preSend(message, messageChannel))
//                    .isInstanceOf(BusinessException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_UNAUTHORIZED_ACCESS);
//
//            then(chatMessageService).should(times(1)).canSendMessage(USER_ID, ROOM_ID);
//        }
//    }
//
//    @Nested
//    @DisplayName("DISCONNECT 커맨드 테스트")
//    class DisconnectCommandTest {
//
//        @Test
//        @DisplayName("[성공] DISCONNECT 커맨드는 특별한 로직 없이 무시됨")
//        void handleDisconnect_Ignored() {
//            // Given
//            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
//            accessor.setSessionAttributes(new HashMap<>());
//
//            Message<byte[]> message = createMessage(accessor);
//
//            // When
//            Message<?> result = chatPreHandler.preSend(message, messageChannel);
//
//            // Then
//            assertThat(result).isNotNull();
//            then(chatRoomService).should(never()).isUserInRoom(anyLong(), anyLong());
//        }
//    }
//}