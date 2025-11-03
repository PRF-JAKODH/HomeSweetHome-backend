package com.homesweet.homesweetback.domain.chat.repository;

import com.homesweet.homesweetback.domain.chat.dto.ChatMessageDto;
import com.homesweet.homesweetback.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


// 메시지 CRUD, 메시지 조회 쿼리
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {


    // 최신 메세지 로드
    Slice<ChatMessage>findByRoomIdOrderBySentAtDesc(Long roomId, Pageable pageable);


    // 추가 로드
    @Query("SELECT m FROM ChatMessage m " +
            "WHERE m.room.id = :roomId " +
            "AND m.id < : lastMessageId " +
            "ORDER BY m.sentAt DESC")
    Slice<ChatMessage>findOlderMessages(
            @Param("roomId") Long roomId,
            @Param("lastMessageId") Long lastMessageId,
            Pageable pageable
    );


}

