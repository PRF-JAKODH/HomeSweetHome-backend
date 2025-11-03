package com.homesweet.homesweetback.domain.chat.repository;

import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;
// 채팅방 생성  삭제
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByTypeAndPairKey(ChatRoomType type, String pairKey);

    boolean existsByTypeAndPairKey(ChatRoomType type, String pairKey);

}



