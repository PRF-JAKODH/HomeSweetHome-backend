package com.homesweet.homesweetback.domain.chat.service;

import com.homesweet.homesweetback.domain.chat.dto.response.ChatMessageResponse;

public interface ChatMessageService {

    /**
     * 채팅 메시지 전송 및 저장
     */
    ChatMessageResponse sendMessage(Long roomId, Long senderId, String content);

    /**
     * 채팅방 메시지 읽음 처리
     */
    void markAsRead(Long roomId, Long userId, Long lastReadMessageId);

    void checkMember(Long subRoomId, Long subUser);
}
