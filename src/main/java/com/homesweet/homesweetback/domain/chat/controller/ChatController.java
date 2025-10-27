package com.homesweet.homesweetback.domain.chat.controller;

import com.homesweet.homesweetback.domain.chat.dto.ChatMessageDTO;
import com.homesweet.homesweetback.domain.chat.dto.ChatSendRequest;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

// 메세지 송수신 컨트롤러
@Controller
public class ChatController {

    private final SimpMessagingTemplate template; // 보내는 객체
    private final AtomicLong msgSeq = new AtomicLong(1);

    public ChatController(SimpMessagingTemplate template) {
        this.template = template;
    }

    @MessageMapping("/chat.send")
    public void send(@Payload ChatSendRequest req){

        ChatMessageDTO msg = new ChatMessageDTO(
                msgSeq.getAndIncrement(),
                req.roomId(),
                req.senderId(),
                req.text(),
                Instant.now().toString()
        );
        // 이 방을 구독중인 모든 사용자에게 메시지 전송(= 브로드캐스트)
        template.convertAndSend("/topic/rooms/" + req.roomId(), msg);
    }

}


