package com.homesweet.homesweetback.domain.chat.service.Imp;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.s3.impl.S3ImageUploader;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.chat.dto.RoomDto;
import com.homesweet.homesweetback.domain.chat.dto.request.CreateGroupRoomRequest;
import com.homesweet.homesweetback.domain.chat.dto.response.*;
import com.homesweet.homesweetback.domain.chat.entity.ChatMessage;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatUserRole;
import com.homesweet.homesweetback.domain.chat.mapper.ChatRoomMapper;
import com.homesweet.homesweetback.domain.chat.repository.ChatMessageRepository;
import com.homesweet.homesweetback.domain.chat.repository.ChatRoomRepository;

import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
import com.homesweet.homesweetback.domain.chat.service.ChatRoomService;
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
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMapper chatRoomMapper;
    private final S3ImageUploader s3ImageUploader;


    /**
     * 개인 채팅방 생성
     */
    @Override
    @Transactional
    public RoomDto createOrGetIndividualRoom(Long meId, Long targetId) {

        if (meId == null || targetId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "meId와 targetId가 필요합니다.");
        }
        if (meId.equals(targetId)) {
            throw new ResponseStatusException(BAD_REQUEST, "자기 자신과는 1:1 채팅을 만들 수 없습니다.");
        }

        String pairKey = buildPairKey(meId, targetId);
        Optional<ChatRoom> existing = chatRoomRepository.findByTypeAndPairKey(ChatRoomType.INDIVIDUAL, pairKey);

        if  (existing.isPresent()) {
            ChatRoom chatRoom = existing.get();
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
        chatRoomRepository.saveAndFlush(room);


        User me = userRepository.getReferenceById(meId);
        User target = userRepository.getReferenceById(targetId);

        // 채팅 요청 주체
        roomMemberRepository.save(RoomMember.builder()
                .room(room)
                .user(me)
                .role(ChatUserRole.OWNER)
                .isExit(false)
                .build());

        // 채팅 요청 받는 사람
        roomMemberRepository.save(RoomMember.builder()
                .room(room)
                .user(target)
                .role(ChatUserRole.MEMBER)
                .isExit(false)
                .build());

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
    @Transactional
    public GroupRoomCreateResponse createGroupRoom(Long ownerId, CreateGroupRoomRequest request) {

        User owner = userRepository.findById(ownerId)
               .orElseThrow(() -> new RuntimeException("User not found"));

        String thumbnailUrl = s3ImageUploader.upload(request.roomThumbnailUrl(),"group/chat/thumbnail");

        // 요청 받아온 dto를 엔터티 객체로 생성
        ChatRoom chatRoom = chatRoomMapper.toEntity(request, thumbnailUrl);

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
     * 개인 채팅방 상세 조회 (멤버 자동 등록/재입장 포함)
     */
    @Override
    @Transactional
    public IndividualChatDetailResponse getIndividualChatDetail(Long userId, Long roomId) {

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
                log.info("roomId: {}, userId: {}", roomId, userId);

        // 방 타입 확인: 개인방(Individual/Private)이 아니면 예외 처리
        if (!chatRoom.getType().equals(ChatRoomType.INDIVIDUAL)) {
            throw new BusinessException(ErrorCode.INVALID_ROOM_TYPE);
        }

        log.info("방타입 확인 roomId: {}, userId: {}", roomId, userId);
        // 1. 멤버 확인 및 자동 등록/재입장 처리 (ensureRoomMembership 재사용)
        ensureRoomMembership(chatRoom, userId);

        // 2.  findPartnerUserInRoom 쿼리를 사용하여 상대방 User를 바로 조회
        User partner = roomMemberRepository.findPartnerUserInRoom(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_MEMBER_NOT_FOUND));

        // 3. 응답 DTO 생성
        return IndividualChatDetailResponse.builder()
                .roomId(roomId)
                .partnerId(partner.getId())
                .partnerName(partner.getName())
                .partnerProfileImageUrl(partner.getProfileImageUrl())
                .build();
    }


    /**
     * 그룹 채팅방 상세 조회 (멤버 자동 등록/재입장 포함)
     */
    @Override
    @Transactional
    public GroupChatDetailResponse getGroupChatDetail(Long userId, Long roomId) {

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        // 방 타입 확인: 그룹방이 아니면 예외 처리
        if (!chatRoom.getType().equals(ChatRoomType.GROUP)) {
            throw new BusinessException(ErrorCode.INVALID_ROOM_TYPE);
        }

        // 1. 멤버 확인 및 자동 등록/재입장 처리 (ensureRoomMembership 재사용)
        ensureRoomMembership(chatRoom, userId);

        // 2. 퇴장하지 않은 모든 활성 멤버 조회
        List<RoomMember> activeMembers = roomMemberRepository.findByRoom_IdAndIsExitFalse(roomId);

        // 3. 참여자 User 정보를 DTO List로 변환
        List<GroupChatDetailResponse.MemberInfo> participants = activeMembers.stream()
                .map(member -> GroupChatDetailResponse.MemberInfo.builder()
                        .userId(member.getUser().getId())
                        .userName(member.getUser().getName())
                        .profileUrl(member.getUser().getProfileImageUrl())
                        .build())
                .toList();

        // 4. 응답 DTO 생성 및 반환
        return GroupChatDetailResponse.builder()
                .roomId(chatRoom.getId())
                .roomName(chatRoom.getName())
                .roomThumbnailUrl(chatRoom.getThumbnailUrl()) // ChatRoom에 해당 필드가 있다고 가정
                .memberCount(participants.size()) // Long to Integer 변환 대신 List size 사용
                .participants(participants)
                .build();
    }

    // ChatRoomService 내부에 private 또는 protected로 정의
    private RoomMember ensureRoomMembership(ChatRoom chatRoom, Long userId) {
        Optional<RoomMember> memberOptional = roomMemberRepository.findByRoomIdAndUserId(chatRoom.getId(), userId);

        if (memberOptional.isEmpty()) {
            //  1. 멤버가 없으면 신규 생성 (자동 등록)
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            RoomMember newMember = RoomMember.createMember(chatRoom, user, ChatUserRole.MEMBER);
            return roomMemberRepository.save(newMember);
        }

        // 2. 멤버가 존재함
        RoomMember member = memberOptional.get();

        // 3. 퇴장 상태면 재입장 처리 (자동 재입장)
        if (member.getIsExit()) {
            member.join(); // @Transactional에 의해 DB에 반영됨
            log.info("재입장 처리 - roomId: {}, userId: {}", chatRoom.getId(), userId);
        }

        return member;
    }


    /**
     * 채팅방 입장
     */
    @Transactional
    @Override
    public void joinRoom(Long roomId, Long userId) {
        log.info("채팅방 입장 시도 - roomId: {}, userId: {}", roomId, userId);

        Optional<RoomMember> memberOptional = roomMemberRepository.findByRoomIdAndUserId(roomId, userId);

        if (memberOptional.isEmpty()) {
            throw new BusinessException(ErrorCode.ROOM_MEMBER_NOT_FOUND);
        }

        RoomMember member = memberOptional.get();
        member.join();

        log.info("채팅방 입장 - roomId: {}, userId: {}", roomId, userId);
    }

    /**
     * 채팅방 퇴장
     */
    @Transactional
    @Override
    public void exitRoom(Long roomId, Long userId) {
        log.info("채팅방 퇴장 시도 - roomId: {}, userId: {}", roomId, userId);

        Optional<RoomMember> memberOptional = roomMemberRepository.findByRoomIdAndUserId(roomId, userId);

        if (memberOptional.isEmpty()) {
            throw new BusinessException(ErrorCode.ROOM_MEMBER_NOT_FOUND);
        }

        RoomMember member = memberOptional.get();
        member.exit();

        log.info(" 채팅방 퇴장 - roomId: {}, userId: {}", roomId, userId);

        // 그룹 채팅방인 경우 자동 삭제 체크
        Optional<ChatRoom> roomOptional = chatRoomRepository.findById(roomId);
        if (roomOptional.isEmpty()) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }

        ChatRoom room = roomOptional.get();

        if (room.getType().equals(ChatRoomType.GROUP)) {
            checkAndDeleteEmptyGroupRoom(room, roomId);
        }
    }

    /**
     * 빈 그룹 채팅방 자동 삭제
     */
    private void checkAndDeleteEmptyGroupRoom(ChatRoom room, Long roomId) {
        List<RoomMember> activeMember = roomMemberRepository.findByRoom_IdAndIsExitFalse(roomId);

        if (activeMember.isEmpty()) {
            room.delete();
            log.warn("그룹 채팅방 자동 삭제 - roomId: {}, roomName: {} (활성 멤버 0명)",
                    room.getId(), room.getName());
        } else {
            log.info("그룹 채팅방 활성 멤버 수 - roomId: {}, count: {}명",
                    room.getId(), activeMember.size());
        }
    }

    /**
     * 내가 속한 1:1 채팅방 목록 조회
     */
    @Override
    public List<IndividualRoomListResponse> findMyIndividualRooms(Long userId) {
        return roomMemberRepository.findMyIndividualRoomList(userId);
    }

    /**
     * 내가 속한 그룹 채팅방 목록 조회
     */
    @Override
    public List<GroupRoomListResponse> findMyGroupRooms(Long userId) {
        return roomMemberRepository.findMyGroupRoomList(userId);
    }

    @Override
    public boolean isUserInRoom(Long userId, Long roomId) {
        return roomMemberRepository.existsByRoom_IdAndUser_IdAndIsExitFalse(roomId, userId);
    }

    @Override
    public List<GroupRoomListResponse> getAllGroupRooms() {
        List<ChatRoom> groupRooms = chatRoomRepository.findByType(ChatRoomType.GROUP);

        return groupRooms.stream()
                .map(room -> {
                    // 마지막 메시지 조회
                    ChatMessage lastMessage = chatMessageRepository
                            .findTopByRoomOrderBySentAtDesc(room)
                            .orElse(null);

                    // 방 참여자 수 계산
                    Long memberCount = roomMemberRepository.countByRoomId(room.getId());

                    // 매퍼로 DTO 변환
                    return chatRoomMapper.toGroupRoomListDto(room, lastMessage, memberCount);
                })
                .toList();
    }


}
