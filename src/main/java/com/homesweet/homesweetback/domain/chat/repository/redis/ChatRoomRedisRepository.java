package com.homesweet.homesweetback.domain.chat.repository.redis;

import com.homesweet.homesweetback.domain.chat.entity.ChatMessage;

import java.time.LocalDateTime;

public interface ChatRoomRedisRepository {

    void saveLastMessage(Long roomId, String content, LocalDateTime sentAt);

//    String getLastMessage(Long roomId);

//    LocalDateTime getLastMessageTime(Long roomId);
}
