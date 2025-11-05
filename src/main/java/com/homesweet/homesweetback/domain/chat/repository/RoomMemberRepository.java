package com.homesweet.homesweetback.domain.chat.repository;

import com.homesweet.homesweetback.domain.chat.dto.response.RoomListCommonResponseDto;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    /**
     * 특정 채팅방의 특정 사용자 멤버 조회
     */
//    Optional<RoomMember> findByRoomAndUser(ChatRoom room, User user);

    /**
     * 특정 채팅방의 특정 사용자 멤버 정보 조회
     */
    Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId);

    /**
     * 사용자가 특정 채팅방의 멤버인지 확인
     */
    boolean existsByRoomIdAndUserId(Long roomId, Long userId);


    /**
     * 사용자가 특정 채팅방의 멤버인지 확인
     */
    List<RoomMember> findAllByRoomId(Long roomId);

//    /**
//     * 내가 속한 1:1 채팅방 목록 조회 (상대방 정보 포함)
//     */
    @Query("""
        SELECT new com.homesweet.homesweetback.domain.chat.dto.response.RoomListCommonResponseDto(
            r.id,
            r.name,
            r.type,
            COUNT(m),
            partner.user.id,
            partner.user.name,
            COALESCE(partner.user.profileImageUrl, ''),
            COALESCE(lastMsg.content, ''),
            lastMsg.sentAt,
            lastMsg.id,
            CASE WHEN my.lastReadId >= lastMsg.id THEN true ELSE false END
        )
        FROM RoomMember my
        JOIN my.room r
        JOIN RoomMember partner 
            ON partner.room.id = r.id 
           AND partner.user.id != :myUserId
        JOIN RoomMember m 
            ON m.room.id = r.id
        LEFT JOIN ChatMessage lastMsg 
            ON lastMsg.id = (
                SELECT MAX(msg.id)
                FROM ChatMessage msg
                WHERE msg.room.id = r.id
            )                                           
        WHERE my.user.id = :myUserId
          AND (r.type = :roomType OR :roomType IS NULL)
          AND (my.isExit = false OR my.isExit IS NULL)
        GROUP BY r.id, r.name, r.type, partner.user.id, partner.user.name, partner.user.profileImageUrl, lastMsg.id, my.lastReadId
        ORDER BY COALESCE(lastMsg.sentAt, r.createdAt) DESC
    """)
    List<RoomListCommonResponseDto> findMyRoomsByType(
            @Param("myUserId") Long myUserId,
            @Param("roomType") ChatRoomType roomType
    );

    /**
     * 내가 속한 채팅방의 id와 나의 id
     */
    RoomMember findByUserIdAndRoomId (Long userId, Long roomId);

    // ✅ 유저가 해당 방에 속해 있고 퇴장하지 않았는지 확인
    boolean existsByRoom_IdAndUser_IdAndIsExitFalse(Long roomId, Long userId);

    // ✅ 특정 방에서 OWNER인지 확인 (삭제 권한 검증용)
    boolean existsByRoom_IdAndUser_IdAndRole(Long roomId, Long userId, ChatUserRole role);

    // ✅ 방 멤버 목록 조회
    List<RoomMember> findByRoom_IdAndIsExitFalse(Long roomId);

}

