package com.homesweet.homesweetback.common.config.interceptor;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.security.jwt.JwtAuthenticationFilter;
import com.homesweet.homesweetback.common.security.jwt.JwtTokenProvider;
import com.homesweet.homesweetback.domain.chat.service.ChatMessageService;
import com.homesweet.homesweetback.domain.chat.service.ChatRoomService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPreHandler implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectProvider<ChatRoomService> chatRoomServiceProvider;
    private final ObjectProvider<ChatMessageService> chatMessageServiceProvider;

    @Value("${test.mode:false}")
    private boolean testMode;  // application.yml에서 설정


    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        log.info(" Remote message {} : ", message);
        log.info(" channel {} : ", channel);


        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);


        if (accessor == null || accessor.getCommand() == null) return message;

        StompCommand command = accessor.getCommand();

        try {
            switch (command) {

                /**
                 * CONNECT — 최초 연결 시 JWT 토큰 검증 & 세션에 사용자 등록
                 */
                case CONNECT -> handleConnect(accessor);

                /**
                 *  SUBSCRIBE — 구독 요청 시 방 참여자 여부 검증
                 */
                case SUBSCRIBE -> handleSubscribe(accessor);

                /**
                 *  SEND — 메시지 전송 시 권한 검증 (퇴장 여부 등)
                 */
                case SEND -> handleSend(accessor);

                default -> { /* 나머지 명령어는 무시 */ }
            }

        } catch (Exception e) {
            log.error(" WebSocket Interceptor Error: {}", e.getMessage());
            throw e;
        }
        return message;
    }

    /**
     * CONNECT 단계 처리 (JWT 인증)
     */
    private void handleConnect(StompHeaderAccessor accessor) {

        String authorization = accessor.getFirstNativeHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        String token = authorization.substring(7);
        Long userId;

        if (isTestToken(token)) {
            // true 이면 테스트 토큰 처리
            userId = Long.parseLong(token);
            log.info(" 테스트 토큰 인증 | userId={}", userId);

        } else {
            // false이면 실제 JWT 검증
            Claims claims =jwtTokenProvider.getClaimsFromToken(token);
            userId = Long.valueOf(claims.getSubject());
            log.info(" JWT 인증 | userId={}", userId);
        }

        accessor.getSessionAttributes().put("userId", userId);

    }

    /**
     * SUBSCRIBE 단계 처리 (구독 권한 검증)
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {
        Long userId = (Long) accessor.getSessionAttributes().get("userId");
        Long roomId = extractRoomId(accessor.getDestination());

        if (userId == null) {
            throw new BusinessException(ErrorCode.MESSAGE_INVALID_REQUEST);
        }

        // 테스트 모드에서는 방 참여 검증 스킵
        if (testMode) {
            log.info(" [TEST MODE] SUBSCRIBE 허용 | userId={} | roomId={}", userId, roomId);
            return;
        }

        // 실제 빈
        ChatRoomService chatRoomService = chatRoomServiceProvider.getObject();

        boolean isMember = chatRoomService.isUserInRoom(roomId, userId);
        if (!isMember) {
            throw new BusinessException(ErrorCode.MESSAGE_UNAUTHORIZED_ACCESS);
        }

        log.info(" SUBSCRIBE 성공 | userId={} | roomId={}", userId, roomId);
    }

    /**
     * SEND 단계 처리 (메시지 전송 권한 검증)
     */
    private void handleSend(StompHeaderAccessor accessor) {

//        ChatMessageService chatMessageService = chatMessageServiceProvider.getObject();

        Long userId = (Long) accessor.getSessionAttributes().get("userId");

        if (userId == null) {
            throw new BusinessException(ErrorCode.MESSAGE_INVALID_REQUEST);
        }

        // 테스트 모드에서는 추가 검증 스킵
        if (testMode) {
            log.debug(" [TEST MODE] SEND 허용 | userId={}", userId);
        }

    }

    /**
     * Destination 문자열에서 roomId 추출
     * ex) /sub/rooms/12 → 12
     */
    private Long extractRoomId(String destination) {
        if (destination == null) return null;
        try {
            return Long.parseLong(destination.substring(destination.lastIndexOf('/') + 1));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isTestToken(String token) {
        if (token == null || token.isEmpty()) return false;

        try {
            Long.parseLong(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}