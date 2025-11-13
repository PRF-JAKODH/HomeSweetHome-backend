package com.homesweet.homesweetback.domain.chat.service;

import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;

public interface RoomMemberService {

    void registerGroupMember(ChatRoom chatRoom, Long userId);

    String buildPairKey(Long a, Long b);

    void registerIndividualMember(ChatRoom room, Long meId, Long targetId);

}