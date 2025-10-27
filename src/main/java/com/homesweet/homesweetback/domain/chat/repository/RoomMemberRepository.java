package com.homesweet.homesweetback.domain.chat.repository;

import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
// 방-사용자 관계, 멤버 확인, 퇴장 처리
public interface RoomMemberRepository {

    // 존재 여부만 확인 (보안용)
    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    // 단일 멤버 조회 (읽음 처리, 퇴장 여부)
    Optional<ChatRoom> findByRoomIdAndUserId(Long roomId, Long userId);


    // 참여자 목록 조회 (상대방 찾기, 전체 멤버 보기)
    List<RoomMember> findByRooId(Long roomId,Long userId);

}
