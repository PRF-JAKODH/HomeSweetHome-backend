package com.homesweet.homesweetback.domain.chat.service.Imp;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatUserRole;
import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
import com.homesweet.homesweetback.domain.chat.service.RoomMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class RoomMemberServiceImpl implements RoomMemberService {


    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void registerGroupMember(ChatRoom chatRoom, Long userId) {
        Optional<RoomMember> memberOptional = roomMemberRepository.findByRoomIdAndUserId(chatRoom.getId(), userId);

        if (memberOptional.isEmpty()) {
            //  1. 멤버가 없으면 신규 생성 (자동 등록)
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            RoomMember newMember = RoomMember.createMember(chatRoom, user, ChatUserRole.MEMBER);
            roomMemberRepository.save(newMember);
            return ;
        }

        // 2. 멤버 받아옴
        RoomMember member = memberOptional.get();

//        // 3. 퇴장 상태면 재입장 처리 (자동 재입장)
        if (member.isExit()) {
            member.join();
            log.info("재입장 처리 - roomId: {}, userId: {}", chatRoom.getId(), userId);
        }
    }

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

}
