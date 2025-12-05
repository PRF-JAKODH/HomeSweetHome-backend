package com.homesweet.homesweetback.domain.chat.service.Imp;

import com.homesweet.homesweetback.domain.chat.repository.redis.ChatRoomRedisRepository;
import com.homesweet.homesweetback.domain.chat.service.ChatRoomMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatRoomMetadataServiceImpl implements ChatRoomMetadataService {

    private final ChatRoomRedisRepository redisRepository;

    @Override
    public void updateLastMessage(Long roomId, String content, LocalDateTime sentAt) {

        redisRepository.saveLastMessage(roomId, content, sentAt);


    }
}
