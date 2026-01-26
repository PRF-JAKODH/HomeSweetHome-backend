package com.homesweet.homesweetback.domain.chat.service;

import com.homesweet.homesweetback.domain.chat.dto.response.RoomMemberResponse;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatUserRole;

import java.util.Optional;

public interface RoomMemberService {

    String buildPairKey(Long a, Long b);

    void registerIndividualMember(ChatRoom room, Long meId, Long targetId);

    /**
     * 신규 멤버 등록
     * @return 등록된 멤버 정보 (DTO)
     */
    RoomMemberResponse registerNewMember(Long roomId, Long userId, ChatUserRole role);

    /**
     * 멤버 재입장 처리
     */
    RoomMemberResponse rejoinMember(Long roomId, Long userId);


}