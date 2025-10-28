package com.homesweet.homesweetback.domain.chat.controller;

import com.homesweet.homesweetback.domain.chat.dto.ChatMessageDto;
import com.homesweet.homesweetback.domain.chat.dto.request.ChatSendRequest;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Controller
public class ChatController {

    private final SimpMessagingTemplate template; // 보내는 객체
    private final AtomicLong msgSeq = new AtomicLong(1);

    public ChatController(SimpMessagingTemplate template) {
        this.template = template;
    }

    @MessageMapping("/chat/sendMessage")
    public void send(@Payload ChatSendRequest req){

        ChatMessageDto msg = new ChatMessageDto(
                msgSeq.getAndIncrement(),
                req.roomId(),
                req.senderId(),
                req.text(),
                LocalDateTime.now()
        );
        // 이 방을 구독중인 모든 사용자에게 메시지 전송(= 브로드캐스트)
        template.convertAndSend("/topic/rooms/" + req.roomId(), msg);
    }

}


