package com.homesweet.homesweetback.domain.chat.controller;


import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.chat.dto.request.ChatReadRequest;
import com.homesweet.homesweetback.domain.chat.dto.request.ChatSendRequest;
import com.homesweet.homesweetback.domain.chat.dto.response.ChatMessageSendResponse;
import com.homesweet.homesweetback.domain.chat.dto.response.PreMessageResponse;
import com.homesweet.homesweetback.domain.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(
            @Payload ChatSendRequest request) {

        Long senderId = request.senderId();
        Long roomId = request.roomId();

        if (senderId == null) {
            throw new BusinessException(ErrorCode.MESSAGE_UNAUTHORIZED_ACCESS);
        }

        ChatMessageSendResponse savedMessage = chatMessageService.sendMessage(
                roomId,
                senderId,
                request.content()
        );
        //  방 전체 구독자에게 메시지 전송
        String destination = "/sub/chat/rooms/" + roomId;
        messagingTemplate.convertAndSend(destination, savedMessage);

    }

    /**
     * 채팅방 메시지 읽음 처리
     */
//    @MessageMapping("/chat.read")
//    public void markMessagesAsRead(
//            @Payload ChatReadRequest request,
//            SimpMessageHeaderAccessor headerAccessor) {
//
//        try {
//            // 세션에서 사용자 ID 추출
//            Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
//
//            // 읽음 처리
//            chatMessageService.markAsRead(
//                    request.roomId(),
//                    userId,
//                    request.lastReadMessageId()
//            );
//        } catch (Exception e) {
//            log.error("읽음 처리 실패: {}", e.getMessage());
//        }
//    }

}


