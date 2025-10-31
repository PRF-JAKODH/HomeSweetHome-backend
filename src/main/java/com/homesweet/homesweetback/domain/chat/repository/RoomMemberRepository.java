package com.homesweet.homesweetback.domain.chat.repository;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.chat.dto.response.RoomListResponseDto;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
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
//    boolean existsByRoomIdAndUserId(Long roomId, Long userId);


    /**
     * 사용자가 특정 채팅방의 멤버인지 확인
     */
    List<RoomMember> findAllByRoomId(Long roomId);

    /**
     * 내가 속한 1:1 채팅방 목록 조회 (상대방 정보 포함)
     */
    @Query("""
        SELECT new com.homesweet.homesweetback.domain.chat.dto.response.RoomListResponseDto(
            r.id,
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
        JOIN RoomMember partner ON partner.room.id = r.id AND partner.user.id != :myUserId
        LEFT JOIN ChatMessage lastMsg ON lastMsg.id = r.lastMessageId
        WHERE my.user.id = :myUserId
          AND r.type = 'INDIVIDUAL'
          AND (my.isExit = false OR my.isExit IS NULL)
        ORDER BY COALESCE(lastMsg.sentAt, r.createdAt) DESC
    """)
    List<RoomListResponseDto> findMyIndividualRoomsWithPartner(@Param("myUserId") Long myUserId);

    /**
     * 내가 속한 채팅방의 id와 나의 id
     */
    RoomMember findByUserIdAndRoomId (Long userId, Long roomId);
}

