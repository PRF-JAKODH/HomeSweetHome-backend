package com.homesweet.homesweetback.domain.chat.service;

import java.time.LocalDateTime;

public interface ChatRoomMetadataService {

    void updateLastMessage(Long roomId, String content, LocalDateTime sentAt);
}
