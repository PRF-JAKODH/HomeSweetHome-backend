package com.homesweet.homesweetback.domain.chat.service;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.chat.dto.RoomDto;
import com.homesweet.homesweetback.domain.chat.dto.request.CreateGroupRoomRequest;
import com.homesweet.homesweetback.domain.chat.dto.response.ChatRoomDetailResponse;
import com.homesweet.homesweetback.domain.chat.dto.response.RoomListCommonResponseDto;
import com.homesweet.homesweetback.domain.chat.dto.response.GroupRoomResponse;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatUserRole;
import com.homesweet.homesweetback.domain.chat.mapper.ChatRoomMapper;
import com.homesweet.homesweetback.domain.chat.repository.ChatRoomRepository;

import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final ChatRoomMapper chatRoomMapper;


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

        // 방 재사용
        if  (existing.isPresent()) {
            ChatRoom chatRoom = existing.get();
            log.info("📎 기존 방 재사용: roomId={}, pairKey={}", chatRoom.getId(), pairKey);
            return RoomDto.builder()
                    .roomId(chatRoom.getId())
                    .reused(true)
                    .build();
        }

        // 방 생성
        ChatRoom room = ChatRoom.builder()
                .type(ChatRoomType.INDIVIDUAL)
                .name("INDIVIDUAL-" + pairKey)
                .pairKey(pairKey)
                .build();
        chatRoomRepository.saveAndFlush(room); // roomId 즉시 생성 보장

        User me = userRepository.getReferenceById(meId);
        User target = userRepository.getReferenceById(targetId);

        roomMemberRepository.save(RoomMember.builder()
                .room(room)
                .user(me)
                .role(ChatUserRole.OWNER)
                .isExit(false)
                .build());

        roomMemberRepository.save(RoomMember.builder()
                .room(room)
                .user(target)
                .role(ChatUserRole.MEMBER)
                .isExit(false)
                .build());

        log.info(" RoomMember 2명 등록 완료: roomId={}, meId={}, targetId={}", room.getId(), meId, targetId);

        return RoomDto.builder()
                .roomId(room.getId())
                .type(room.getType().name())
                .name(room.getName())
                .pairKey(room.getPairKey())
                .reused(false)
                .build();
    }

    private String buildPairKey(Long a, Long b) {
        long low = Math.min(a, b);
        long high = Math.max(a, b);
        return low + ":" + high;
    }

    /**
     * 그룹 채팅방 생성
     */

    @Override
    public GroupRoomResponse createGroupRoom(Long ownerId, CreateGroupRoomRequest request) {

        User owner = userRepository.findById(ownerId)
               .orElseThrow(() -> new RuntimeException("User not found"));


        // 요청 받아온 dto를 엔터티 객체로 생성
        ChatRoom chatRoom = chatRoomMapper.toEntity(request);

        // 채팅방 정보 저장
        chatRoomRepository.save(chatRoom);

        // 방장 정보 저장
        roomMemberRepository.save(RoomMember.builder()
                .room(chatRoom)
                .user(owner)
                .role(ChatUserRole.OWNER)
                .isExit(false)
                .build()
        );

        // 저장된 정보 응답
        return chatRoomMapper.toDto(chatRoom, ownerId);

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


    /**
     * 내가 속한 채팅방 전체 목록 조회
     */
    public List<RoomListCommonResponseDto> findAllMyRooms(Long userId) {

        List<RoomListCommonResponseDto> rooms = roomMemberRepository.findMyRoomsByType(userId, null);

        return rooms.stream()
                .filter(room -> !room.getLastMessageIsRead())
                .collect(Collectors.toList());
    }

    /**
     * 내가 속한 1:1 채팅방 목록 조회
     */
    public List<RoomListCommonResponseDto> findMyIndividualRooms(Long userId) {
        return roomMemberRepository.findMyRoomsByType(userId, ChatRoomType.INDIVIDUAL);
    }

    /**
     * 내가 속한 그룹 채팅방 목록 조회
     */
    public List<RoomListCommonResponseDto> findMyGroupRooms(Long userId) {
        return roomMemberRepository.findMyRoomsByType(userId, ChatRoomType.GROUP);
    }


    @Override
    public boolean isUserInRoom(Long userId, Long roomId) {
        return roomMemberRepository.existsByRoom_IdAndUser_IdAndIsExitFalse(roomId, userId);
    }


}
