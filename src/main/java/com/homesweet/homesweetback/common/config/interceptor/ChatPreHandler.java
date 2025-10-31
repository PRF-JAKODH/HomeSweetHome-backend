package com.homesweet.homesweetback.common.config.interceptor;

import com.homesweet.homesweetback.common.security.jwt.JwtTokenProvider;
import com.homesweet.homesweetback.domain.chat.service.ChatMessageService;
import com.homesweet.homesweetback.domain.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPreHandler implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel){
        StompHeaderAccessor accessHeader = StompHeaderAccessor.getAccessor(message,StompHeaderAccessor.class);
        if(accessHeader == null || accessHeader.getCommand() == null) return message;

        switch (accessHeader.getCommand()) {
            case CONNECT :
                Long connectUser = (Long)accessHeader.getSessionAttributes().get("userId");
                accessHeader.setUser(new UsernamePasswordAuthenticationToken(connectUser, null, List.of()));
                break;

            case SUBSCRIBE :
                Long subUser = (Long)accessHeader.getSessionAttributes().get("userId");
                Long subRoomId = extractRoomId(accessHeader.getDestination());
                chatMessageService.checkMember(subRoomId, subUser);
                break;

            case SEND:
                Long sendUser = (Long)accessHeader.getSessionAttributes().get("userId");
                break;

            default:
                break;
        }

         return message;
    }

    private Long extractRoomId(String destination) {
        if(destination == null) return null;
        try {
            return Long.parseLong(destination.substring(destination.lastIndexOf('/') + 1));
        }catch(Exception e) {
            return null;
        }

    }

}
////        // 최초 연결 시 — 토큰 인증 (JWT 파싱, 유효성 검사), CONNECT에서만 인증 처리
////        if (!StompCommand.CONNECT.equals(accessHeader.getCommand())) {
////            log.debug("connect");
////
////
////            // Authorization 헤더 추출
////            String authorization = accessHeader.getFirstNativeHeader("Authorization");
////
////            if (authorization == null || !authorization.startsWith("Bearer ")) {
////                throw new BusinessException(ErrorCode.TOKEN_MISSING);
////            }
////
////            // 토큰 분리
////            String token = authorization.substring(7);
////
////            // 토큰 유효성 검증
////            if (!jwtTokenProvider.validateToken(token)) {
////                throw new BusinessException(ErrorCode.TOKEN_INVALID);
////            }
////
////            // 타입 검증 : refresh token이면 예외 (Access만 허용)
////            if (jwtTokenProvider.isRefreshToken(token)) {
////                throw new BusinessException(ErrorCode.TOKEN_REFRESH_NOT_ALLOWED);
////            }
////
////            Claims claims = jwtTokenProvider.getClaimsFromToken(token);
////            Long userId = Long.valueOf(claims.getSubject());
////            String email = claims.get("email", String.class).toString();
////            String role = claims.get("role", String.class).toString();
////
////            // 인증된 사용자 정보
////            Authentication authentication =
////                    new UsernamePasswordAuthenticationToken(
////                            userId,
////                            null,
////                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
////                    );
////
////            accessHeader.setUser(authentication);
////
////            // 구독 요청 - 접근권한 검증
////        } else if (StompCommand.SUBSCRIBE.equals(accessHeader.getCommand())) {
////
////            // 메세지 전송 - 인증된 사용자만 허용
////        } else if(StompCommand.SEND.equals(accessHeader.getCommand())){
////
////        }
////            return message;
//
//
