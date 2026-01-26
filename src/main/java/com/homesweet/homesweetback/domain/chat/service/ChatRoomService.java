package com.homesweet.homesweetback.domain.chat.service;

import com.homesweet.homesweetback.domain.chat.dto.response.IndividualRoomCreateResponse;
import com.homesweet.homesweetback.domain.chat.dto.request.CreateGroupRoomRequest;
import com.homesweet.homesweetback.domain.chat.dto.response.*;

import java.util.List;

public interface ChatRoomService {

    IndividualRoomCreateResponse createOrGetIndividualRoom(Long meId, Long targetId);

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