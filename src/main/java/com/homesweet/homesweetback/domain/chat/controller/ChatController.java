package com.homesweet.homesweetback.domain.chat.controller;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.chat.dto.ChatMessageDto;
import com.homesweet.homesweetback.domain.chat.dto.request.ChatReadRequest;
import com.homesweet.homesweetback.domain.chat.dto.request.ChatSendRequest;
import com.homesweet.homesweetback.domain.chat.dto.response.ChatMessageResponse;
import com.homesweet.homesweetback.domain.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 채팅방 메시지 송수신
     */
    @MessageMapping("/chat.send")
    @SendToUser("/sub/rooms/{roomId}")
    public void sendMessage(
            @Payload ChatSendRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        try {
//            log.debug("Id는 들어옴?" + request);

            String destination = "/sub/rooms/" + request.roomId();
//            log.info("📤 메시지 전송 - destination: {}", destination);

            // 메세지 저장 . 처리
            ChatMessageResponse savedMessage = chatMessageService.sendMessage(
                    request.roomId(),
                    request.senderId(),
                    request.text()
            );

            messagingTemplate.convertAndSend(destination, savedMessage);

            log.info("✅ 브로드캐스트 완료");

        } catch (Exception e) {
            log.error("메시지 전송 실패: {}", e.getMessage());
        }
    }




    /**
     * 채팅방 메시지 읽음 처리
     */
    @MessageMapping("/chat.read")
    public void markMessagesAsRead(
            @Payload ChatReadRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        try {
            // 세션에서 사용자 ID 추출
            Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");

            // 읽음 처리
            chatMessageService.markAsRead(
                    request.roomId(),
                    userId,
                    request.lastReadMessageId()
            );

            // 읽음 알림 전송

        } catch (Exception e) {
            log.error("읽음 처리 실패: {}", e.getMessage());
        }
    }

}


