package com.homesweet.homesweetback.domain.chat.repository;

import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;
// 채팅방 생성 조회 삭제
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByTypeAndPairKey(ChatRoomType type, String pairKey);

    // 그룹 채팅방 전체 조회(비회원/회원)
    List<ChatRoom> findByType(ChatRoomType type);

//    boolean existsByTypeAndPairKey(ChatRoomType type, String pairKey);

}



