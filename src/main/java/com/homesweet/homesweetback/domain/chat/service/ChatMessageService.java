package com.homesweet.homesweetback.domain.chat.service;

import com.homesweet.homesweetback.domain.chat.dto.response.ChatMessageSendResponse;
import com.homesweet.homesweetback.domain.chat.dto.response.PreMessageResponse;

public interface ChatMessageService {

    // 채팅 메시지 전송 및 저장
    ChatMessageSendResponse sendMessage(Long roomId, Long senderId, String content);

    // 이전 메세지 조회 (채팅방 입장 or 스크롤)
    PreMessageResponse getPreMessage(Long roomId, Long lastMessageId, int size);

    boolean canSendMessage(Long userId, Long roomId);

    // 채팅방 메시지 읽음 처리
//    void markAsRead(Long roomId, Long userId, Long lastReadMessageId);

//    void checkMember(Long subRoomId, Long subUser);

}
