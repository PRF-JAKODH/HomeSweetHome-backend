package com.homesweet.homesweetback.domain.chat.repository;

import com.homesweet.homesweetback.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;


// 메시지 CRUD, 메시지 조회 쿼리
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

}

