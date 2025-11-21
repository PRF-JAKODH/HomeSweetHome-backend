package com.homesweet.homesweetback.domain.chat.service.Imp;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.chat.dto.response.GroupChatDetailResponse;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatUserRole;
import com.homesweet.homesweetback.domain.chat.repository.ChatRoomRepository;
import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
import com.homesweet.homesweetback.domain.chat.service.ChatMessageService;
import com.homesweet.homesweetback.domain.chat.service.RoomMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.homesweet.homesweetback.domain.chat.entity.QChatRoom.chatRoom;

@Slf4j
@RequiredArgsConstructor
@Service
public class RoomMemberServiceImpl implements RoomMemberService {


    private final RoomMemberRepository roomMemberRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void registerGroupMember(ChatRoom chatRoom, Long userId) {

        Optional<RoomMember> memberOptional = roomMemberRepository.findByRoomIdAndUserId(chatRoom.getId(), userId);

//        boolean shouldPushUpdate = false;

        if (memberOptional.isEmpty()) {
            // 2. 멤버가 없으면 신규 생성
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            RoomMember newMember = RoomMember.createMember(chatRoom, user, ChatUserRole.MEMBER);
            roomMemberRepository.save(newMember);

//            shouldPushUpdate = true; // 신규 생성되었으므로 푸시 필요

        } else {
            // 3. 퇴장 상태면 재입장 처리
            RoomMember member = memberOptional.get();
            if (member.isExit()) {
                member.join();
//                shouldPushUpdate = true; // 재입장했으므로 푸시 필요
            }
            // (활성 상태면 아무 작업도 하지 않음)
        }
//        if (shouldPushUpdate) {
//            // chatRoom.getId()는 Long 타입이므로, 이를 웹소켓 서비스에 전달
//            eventPublisher.publishEvent(new MemberRegisteredEvent(chatRoom.getId()));
//            log.info("멤버 등록/재입장 완료. 웹소켓 멤버 목록 갱신 푸시!");
//        }

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

    @Override
    @Transactional(readOnly = true)
    public List<GroupChatDetailResponse.MemberInfo> refreshGroupMembers(Long roomId) {

        // 1. [핵심] 사용자가 요청한 전체 활성 멤버 조회 메서드 사용
        List<RoomMember> activeMembers = roomMemberRepository.findByRoom_IdAndIsExitFalse(roomId);

        // 2. DTO로 변환하여 반환
        return activeMembers.stream()
            .map(roomMember -> GroupChatDetailResponse.MemberInfo.builder()
                .userId(roomMember.getUser().getId())
                .userName(roomMember.getUser().getName())
                .profileUrl(roomMember.getUser().getProfileImageUrl())
                .build())
            .collect(Collectors.toList());
    }
}
