package com.homesweet.homesweetback.domain.chat.dto;

import lombok.Getter;

public class MessageResponse {
    private String roomId;      // 채팅방 번호
    private String sender;      // 보내는 사람
    private String message;     // 메시지 내용
}
