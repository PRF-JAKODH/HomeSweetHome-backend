package com.homesweet.homesweetback.common.config.interceptor;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.security.jwt.JwtTokenProvider;
import com.homesweet.homesweetback.domain.chat.repository.ChatRoomRepository;
import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
import com.homesweet.homesweetback.domain.chat.service.ChatMessageService;
import com.homesweet.homesweetback.domain.chat.service.ChatRoomService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPreHandler implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectProvider<ChatRoomService> chatRoomServiceProvider;
    private final ObjectProvider<ChatMessageService> chatMessageServiceProvider;


    // channel mock객체로 둬서 테스트 해보자요
    // send, connect 연결은 메서드 호출해서 사용할 수 있을 듯

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;

        StompCommand command = accessor.getCommand();

        log.info("🔍 Command: {}, SessionId: {}, Destination: {}",
                command, accessor.getSessionId(), accessor.getDestination());

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
            throw new BusinessException(ErrorCode.TOKEN_MISSING);
        }

        String token = authorization.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        if (jwtTokenProvider.isRefreshToken(token)) {
            throw new BusinessException(ErrorCode.TOKEN_REFRESH_NOT_ALLOWED);
        }

        Claims claims = jwtTokenProvider.getClaimsFromToken(token);
        Long userId = Long.valueOf(claims.getSubject());
        String role = claims.get("role", String.class);

        // 세션에 유저 정보 저장
        accessor.getSessionAttributes().put("userId", userId);

        // SecurityContext와 연계되도록 인증 정보 설정
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
        ));

        log.info("CONNECT 성공 | userId={} | role={}", userId, role);
    }

    /**
     * SUBSCRIBE 단계 처리 (구독 권한 검증)
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {
        // 실제 빈
        ChatRoomService chatRoomService = chatRoomServiceProvider.getObject();

        Long userId = (Long) accessor.getSessionAttributes().get("userId");
        Long roomId = extractRoomId(accessor.getDestination());

        if (userId == null) {
            throw new BusinessException(ErrorCode.MESSAGE_INVALID_REQUEST);
        }

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

        ChatMessageService chatMessageService = chatMessageServiceProvider.getObject();

        Long userId = (Long) accessor.getSessionAttributes().get("userId");

        if (userId == null) {
            throw new BusinessException(ErrorCode.MESSAGE_INVALID_REQUEST);
        }

        log.info("SEND 권한 확인 완료 | userId={}", userId);
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
}
