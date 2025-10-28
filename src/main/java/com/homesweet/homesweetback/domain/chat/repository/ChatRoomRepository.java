package com.homesweet.homesweetback.domain.chat.repository;

import com.homesweet.homesweetback.domain.chat.dto.RoomListDto;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
// 채팅방 생성  삭제
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByTypeAndPairKey(ChatRoomType type, String pairKey);

    boolean existsByTypeAndPairKey(ChatRoomType type, String pairKey);


}



