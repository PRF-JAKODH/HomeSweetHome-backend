package com.homesweet.homesweetback.domain.chat.service;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.chat.dto.RoomDto;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatUserRole;
import com.homesweet.homesweetback.domain.chat.repository.ChatMessageRepository;
import com.homesweet.homesweetback.domain.chat.repository.ChatRoomRepository;
import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepo;
    private final ChatMessageRepository chatMessageRepo;
    private final RoomMemberRepository roomMemberRepo;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    @Override
    public RoomDto createOrGetIndividualRoom(Long meId, Long targetId) {

        // 기본 검증
        if (meId == null || targetId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "meId와 targetId가 필요합니다.");
        }
        if (meId.equals(targetId)) {
            throw new ResponseStatusException(BAD_REQUEST, "자기 자신과는 1:1 채팅을 만들 수 없습니다.");
        }

        // pairKey 생성 (유저 쌍 식별용 문자열)
        String pairKey = buildPairKey(meId, targetId);

        //새 ChatRoom 생성
        ChatRoom room = ChatRoom.builder()
                .type(ChatRoomType.INDIVIDUAL)
                .name("INDIVIDUAL-" + pairKey)
                .pairKey(pairKey)
                .lastMessageId(null)
                .thumbnailUrl(null)
                .build();
        // 저장
        chatRoomRepo.save(room);

        User meRef     = em.getReference(User.class, meId);
        User targetRef = em.getReference(User.class, targetId);

        // 내 멤버십 없으면 추가
        if (!roomMemberRepo.existsByRoomIdAndUserId(room.getId(), meId)) {
            RoomMember mine = RoomMember.builder()
                    .room(room)
                    .user(meRef)
                    .role(ChatUserRole.valueOf("OWNER"))
                    .isExit(false)
                    .lastReadId(null)
                    .build();
            roomMemberRepo.save(mine);
        }

        // 상대 멤버십 없으면 추가
        if (!roomMemberRepo.existsByRoomIdAndUserId(room.getId(), targetId)) {
            RoomMember other = RoomMember.builder()
                    .room(room)
                    .user(targetRef)
                    .role(ChatUserRole.valueOf("MEMBER"))
                    .isExit(false)
                    .lastReadId(null)
                    .build();
            roomMemberRepo.save(other);
        }

        //결과 반환
        return RoomDto.builder()
                .roomId(room.getId())
                .type(room.getType().name())
                .name(room.getName())
                .pairKey(room.getPairKey())
                .reused(false)
                .build();
    }
    // 유저 두 명을 정규화해서 pairKey 만들기
    private String buildPairKey(Long a, Long b) {
        long low = Math.min(a, b);
        long high = Math.max(a, b);
        return low + ":" + high;
    }


    // 본인 채팅방 목록 조회
    @Override
    public List<Long> findMyIndividualRoomIds(Long meUserId) {
        return roomMemberRepo.findMyIndividualRoomIds(meUserId);
    }


    // 개인 채팅방 정보 상세조회
//    @Transactional
//    @Override
//    public Page<RoomListDto> listIndividualRooms(Long meUserId, int page, int size) {
//        return roomMemberRepo.findOneToOneRooms(
//                meUserId,
//                ChatRoomType.INDIVIDUAL,
//                PageRequest.of(page, size)
//        );
//    }

}
