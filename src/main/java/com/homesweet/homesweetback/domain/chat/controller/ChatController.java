package com.homesweet.homesweetback.domain.chat.controller;


import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.chat.dto.request.ChatSendRequest;
import com.homesweet.homesweetback.domain.chat.dto.response.ChatMessageSendResponse;
import com.homesweet.homesweetback.domain.chat.event.WebSocketMessage;
import com.homesweet.homesweetback.domain.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

        WebSocketMessage message = new WebSocketMessage(
                "TALK",
                savedMessage
        );

        //  방 전체 구독자에게 메시지 전송
        String destination = "/sub/chat/rooms/" + roomId;
        messagingTemplate.convertAndSend(destination, message);
        log.info(" 메시지 전송 완 roomId={}, senderId={}, messageId={}",
                roomId, senderId, savedMessage.messageId());
    }

}


