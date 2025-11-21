package com.homesweet.homesweetback.domain.chat.service;

import com.homesweet.homesweetback.domain.chat.dto.response.GroupChatDetailResponse;
import com.homesweet.homesweetback.domain.chat.dto.response.MemberInfo;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;

import java.util.List;

public interface RoomMemberService {

    MemberInfo registerGroupMember(Long roomId, Long userId);

    String buildPairKey(Long a, Long b);

    void registerIndividualMember(ChatRoom room, Long meId, Long targetId);

//    List<GroupChatDetailResponse.MemberInfo> refreshGroupMembers(Long roomId);
}