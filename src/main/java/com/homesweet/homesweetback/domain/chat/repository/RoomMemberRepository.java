package com.homesweet.homesweetback.domain.chat.repository;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.chat.dto.response.GroupRoomListResponse;
import com.homesweet.homesweetback.domain.chat.dto.response.IndividualRoomListResponse;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;


public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    /**
     * 특정 채팅방의 특정 사용자 멤버 정보 조회
     */
    Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId);

    // 개인 채팅방 목록 - 더 간단해짐!
    @Query("""
     SELECT new com.homesweet.homesweetback.domain.chat.dto.response.IndividualRoomListResponse(
         r.id,
         r.type,
         CAST(2 AS Long),
         partner.user.id,
         partner.user.name,
         COALESCE(partner.user.profileImageUrl,''),
         r.lastMessage,
         r.lastMessageAt
     )
     FROM RoomMember my
     JOIN my.room r
     JOIN RoomMember partner 
       ON partner.room.id = r.id AND partner.user.id != :myUserId
     WHERE my.user.id = :myUserId
       AND r.type = 'INDIVIDUAL'
       AND (my.isExit = false OR my.isExit IS NULL)
     ORDER BY r.lastMessageAt DESC NULLS LAST
 """)
    List<IndividualRoomListResponse> findMyIndividualRoomList(@Param("myUserId") Long myUserId);

    // 그룹 채팅방 목록
    @Query("""
     SELECT new com.homesweet.homesweetback.domain.chat.dto.response.GroupRoomListResponse(
         r.id,
         r.name,
         r.type,
         r.thumbnailUrl,
         COUNT(m),
         r.lastMessage,
         r.lastMessageAt
     )
     FROM RoomMember my
     JOIN my.room r
     JOIN RoomMember m ON m.room.id = r.id 
     WHERE my.user.id = :myUserId
       AND r.type = 'GROUP'
       AND (my.isExit = false OR my.isExit IS NULL)
     GROUP BY r.id, r.name, r.thumbnailUrl, r.type, r.lastMessage, r.lastMessageAt, r.createdAt
     ORDER BY COALESCE(r.lastMessageAt, r.createdAt) DESC
 """)
    List<GroupRoomListResponse> findMyGroupRoomList(@Param("myUserId") Long myUserId);

    /**
     * 내가 속한 채팅방의 id와 나의 id
     */
    RoomMember findByUserIdAndRoomId (Long userId, Long roomId);

    // 유저가 해당 방에 속해 있고 퇴장하지 않았는지 확인
    boolean existsByRoom_IdAndUser_IdAndIsExitFalse(Long roomId, Long userId);

    // 현재 참여중 방 멤버 목록 조회
    List<RoomMember> findByRoom_IdAndIsExitFalse(Long roomId);

    Long countByRoomId(Long roomId);

    //여러 방의 멤버 수 한번에 조회
    @Query("""
            SELECT rm.room.id, COUNT(rm)
            FROM RoomMember rm
            WHERE rm.room.id IN :roomIds
              AND (rm.isExit = false OR rm.isExit IS NULL)
            GROUP BY rm.room.id
        """)
    Map<Long, Long> countMembersByRoomIds(@Param("roomIds") List<Long> roomIds);

    /**
     * 개인 채팅방에서 상대방 User 찾기 (최적화)
     * 기존: 전체 조회 후 for문 → 개선: 쿼리로 바로 찾기
     */
    @Query("""
            SELECT rm.user
            FROM RoomMember rm
            WHERE rm.room.id = :roomId
              AND rm.user.id != :myUserId
              AND rm.isExit = false
        """)
    Optional<User> findPartnerUserInRoom(
            @Param("roomId") Long roomId,
            @Param("myUserId") Long myUserId
    );

//    List<RoomMember> roomId(Long roomId);
}

