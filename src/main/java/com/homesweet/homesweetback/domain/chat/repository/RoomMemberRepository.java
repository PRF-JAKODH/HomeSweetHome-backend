package com.homesweet.homesweetback.domain.chat.repository;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.chat.dto.RoomListDto;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// 내가 속한 방 조회, 방-사용자 관계, 멤버 확인, 퇴장 처리
public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    boolean existsByRoomAndUser(ChatRoom room, User user);

    Optional<RoomMember> findByRoomAndUser(ChatRoom room, User user);

//    @Query("""
//            select new com.homesweet.homesweetback.domain.chat.dto.RoomListDto(
//              r.id, partner.id, partner.name, r.name, r.thumbnailUrl,
//              lm.content, coalesce(lm.sentAt, r.createdAt),
//              coalesce(lm.id, 0), my.lastReadId
//            )
//            from RoomMember my
//              join my.room r
//              join RoomMember other on other.room = r and other.user.id <> :meUserId
//              join other.user partner
//              left join ChatMessage lm on lm.id = r.lastMessageId
//            where my.user.id = :meUserId
//              and r.type = 'INDIVIDUAL'
//              and (my.isExit = false or my.isExit is null)
//            order by coalesce(lm.id, 0) desc
//            """)
//            Page<RoomListDto> findOneToOneRooms(
//                    @Param("meUserId") Long meUserId,
//                    ChatRoomType individual, Pageable pageable
//            );
//


        @Query("""
        select r.id
        from RoomMember my
        join my.room r
        where my.user.id = :meUserId
          and r.type = 'INDIVIDUAL'
          and (my.isExit = false or my.isExit is null) order by r.id desc
        """)
        List<Long> findMyIndividualRoomIds(@Param("meUserId") Long meUserId);


        boolean existsByRoomIdAndUserId(Long roomId, Long userId);



}
