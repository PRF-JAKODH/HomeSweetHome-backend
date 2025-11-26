package com.homesweet.homesweetback.domain.chat.service;

import com.homesweet.homesweetback.domain.chat.dto.RoomDto;
import com.homesweet.homesweetback.domain.chat.dto.request.CreateGroupRoomRequest;
import com.homesweet.homesweetback.domain.chat.dto.response.*;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import jakarta.transaction.Transactional;

import java.util.List;

public interface ChatRoomService {

    RoomDto createOrGetIndividualRoom(Long meId, Long targetId);

    GroupRoomCreateResponse createGroupRoom(Long ownerId, CreateGroupRoomRequest request);

    IndividualChatDetailResponse getIndividualChatDetail(Long userId, Long roomId);

    GroupChatDetailResponse getGroupChatDetail(Long userId, Long roomId);

    List<IndividualRoomListResponse> findMyIndividualRooms(Long userId);

    List<GroupRoomListResponse> findMyGroupRooms(Long userId);

    boolean isUserInRoom(Long roomId, Long userId);

    List<GroupRoomListResponse> getAllGroupRooms();

    void leaveRoom(Long roomId, Long userId);

    JoinRoomResponse joinRoom(Long roomId, Long userId);


}