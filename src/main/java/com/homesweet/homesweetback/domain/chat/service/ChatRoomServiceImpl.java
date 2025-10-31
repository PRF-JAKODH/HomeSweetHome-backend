package com.homesweet.homesweetback.domain.chat.service;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.chat.dto.RoomDto;
import com.homesweet.homesweetback.domain.chat.dto.response.ChatRoomDetailResponse;
import com.homesweet.homesweetback.domain.chat.dto.response.RoomListResponseDto;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import com.homesweet.homesweetback.domain.chat.repository.ChatRoomRepository;
import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;

    @Transactional
    @Override
    public RoomDto createOrGetIndividualRoom(Long meId, Long targetId) {

        if (meId == null || targetId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "meId와 targetId가 필요합니다.");
        }
        if (meId.equals(targetId)) {
            throw new ResponseStatusException(BAD_REQUEST, "자기 자신과는 1:1 채팅을 만들 수 없습니다.");
        }

        String pairKey = buildPairKey(meId, targetId);

        Optional<ChatRoom> existing = chatRoomRepository.findByTypeAndPairKey(ChatRoomType.INDIVIDUAL, pairKey);

        if (existing.isPresent()) {
            ChatRoom room = existing.get();
            return RoomDto.builder()
                    .roomId(room.getId())
                    .type(room.getType().name())
                    .name(room.getName())
                    .pairKey(room.getPairKey())
                    .reused(true)
                    .build();
        } else {
            ChatRoom room = ChatRoom.builder()
                    .type(ChatRoomType.INDIVIDUAL)
                    .name("INDIVIDUAL-" + pairKey)
                    .pairKey(pairKey)
                    .build();
            chatRoomRepository.save(room);

            return RoomDto.builder()
                    .roomId(room.getId())
                    .type(room.getType().name())
                    .name(room.getName())
                    .pairKey(room.getPairKey())
                    .reused(false)
                    .build();
        }
    }
    // pairKey 생성
    private String buildPairKey(Long a, Long b) {
        long low = Math.min(a, b);
        long high = Math.max(a, b);
        return low + ":" + high;
    }


    /**
     * 내가 속한 1:1 채팅방 목록 조회 (상대방 정보 포함)
     */
    @Override
    public List<RoomListResponseDto> findMyIndividualRooms(Long myUserId) {

        List<RoomListResponseDto> roomList = roomMemberRepository.findMyIndividualRoomsWithPartner(myUserId);

        return roomList;
    }


    /**
     * 내가 속한 1:1 채팅방 상세 조회 (상대방 정보 포함)
     */
    @Override
    public ChatRoomDetailResponse findChatRoomInfo(Long roomId, Long userId) {

        // 채팅방 유/무
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        // 현재 사용자가 멤버인지 확인
        roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new IllegalStateException("채팅방 접근 권한이 없습니다."));

        // 상대방 정보 조회
        List<RoomMember> allMembers = roomMemberRepository.findAllByRoomId(roomId);

        RoomMember partnerMember = allMembers.stream()
                .filter(member -> !member.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("상대방을 찾을 수 없습니다."));

        User partner = partnerMember.getUser();

        log.info("채팅방 정보 조회 완료 - 방: {}, 상대방: {}", roomId, partner.getName());

        //
        return ChatRoomDetailResponse.builder()
                .roomId(chatRoom.getId())
                .partnerId(partner.getId())
                .partnerName(partner.getName())
                .thumbnailUrl(partner.getProfileImageUrl())
                .build();
    }


}
