package com.homesweet.homesweetback.domain.chat.service.Imp;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.chat.dto.response.RoomMemberResponse;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatUserRole;
import com.homesweet.homesweetback.domain.chat.repository.ChatRoomRepository;
import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
import com.homesweet.homesweetback.domain.chat.service.RoomMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class RoomMemberServiceImpl implements RoomMemberService {

    private final RoomMemberRepository roomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void registerIndividualMember(ChatRoom room, Long meId, Long targetId) {
        User me = userRepository.findById(meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<RoomMember> members = List.of(
                RoomMember.createMember(room, me, ChatUserRole.OWNER),
                RoomMember.createMember(room, target, ChatUserRole.MEMBER)
        );
        roomMemberRepository.saveAll(members);
    }

    @Override
    @Transactional
    public String buildPairKey(Long a, Long b) {
        long low = Math.min(a, b);
        long high = Math.max(a, b);
        return low + ":" + high;
    }

    /**
     * 그룹 채팅방 신규 멤버 등록
     */
    @Override
    @Transactional
    public RoomMemberResponse registerNewMember(Long roomId, Long userId, ChatUserRole role) {
        // 1. ChatRoom, User 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 엔티티 생성 및 저장
        RoomMember newMember = RoomMember.createMember(chatRoom, user, role);
        roomMemberRepository.save(newMember);

        log.info("신규 멤버 등록. roomId={}, userId={}, role={}", roomId, userId, role);

        // 4. DTO 변환 후 반환
        return RoomMemberResponse.from(newMember);
    }

    /**
     * 멤버 재입장 처리
     */
    @Transactional
    @Override
    public RoomMemberResponse rejoinMember(Long roomId, Long userId) {
        // 1. 멤버 조회
        RoomMember member = roomMemberRepository
                .findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_MEMBER_NOT_FOUND));

        // 2. 재입장 처리
        if (!member.isExit()) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_ACTIVE);
        }

        member.join();

        log.info("멤버 재입장. roomId={}, userId={}", roomId, userId);

        // 3. DTO 변환 후 반환
        return RoomMemberResponse.from(member);
    }


}
