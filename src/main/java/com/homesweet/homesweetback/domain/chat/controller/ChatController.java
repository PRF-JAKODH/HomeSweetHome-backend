package com.homesweet.homesweetback.domain.chat.controller;


import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.chat.dto.request.ChatReadRequest;
import com.homesweet.homesweetback.domain.chat.dto.request.ChatSendRequest;
import com.homesweet.homesweetback.domain.chat.dto.response.ChatMessageResponse;
import com.homesweet.homesweetback.domain.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;



@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(
            @Payload ChatSendRequest request) {

        log.info("sendMessage request: {}", request);

        // 세션에서 userId 직접 가져오기
        Long senderId = request.senderId();

        if (senderId == null) {
            throw new BusinessException(ErrorCode.MESSAGE_UNAUTHORIZED_ACCESS);
        }

        ChatMessageResponse savedMessage = chatMessageService.sendMessage(
                request.roomId(),
                senderId,
                request.content()
        );
        //  방 전체 구독자에게 메시지 전송
        String destination = "/sub/rooms/" + request.roomId();
        messagingTemplate.convertAndSend(destination, savedMessage);

        log.info(" 메시지 전송 - roomId={}, senderId={}", request.roomId(), senderId);
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
        } catch (Exception e) {
            log.error("읽음 처리 실패: {}", e.getMessage());
        }
    }

}


