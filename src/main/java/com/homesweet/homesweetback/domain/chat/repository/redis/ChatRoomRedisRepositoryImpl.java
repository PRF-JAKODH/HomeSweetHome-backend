package com.homesweet.homesweetback.domain.chat.repository.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class ChatRoomRedisRepositoryImpl implements ChatRoomRedisRepository{

    private final RedisTemplate<String, Object> redisTemplate;

    public ChatRoomRedisRepositoryImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String key(Long roomId) {
        return "chatroom:" + roomId;
    }

    public void saveLastMessage(Long roomId, String content, LocalDateTime sentAt) {
        redisTemplate.opsForHash().put(key(roomId), "lastMessage", content);
        redisTemplate.opsForHash().put(key(roomId), "sentAt", sentAt);
    }
}
