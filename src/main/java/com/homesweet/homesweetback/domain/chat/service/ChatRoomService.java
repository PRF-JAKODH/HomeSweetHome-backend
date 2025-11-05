package com.homesweet.homesweetback.domain.chat.service;

import com.homesweet.homesweetback.domain.chat.dto.RoomDto;
import com.homesweet.homesweetback.domain.chat.dto.request.CreateGroupRoomRequest;
import com.homesweet.homesweetback.domain.chat.dto.response.ChatRoomDetailResponse;
import com.homesweet.homesweetback.domain.chat.dto.response.GroupRoomResponse;
import com.homesweet.homesweetback.domain.chat.dto.response.RoomListCommonResponseDto;

import java.util.List;

public interface ChatRoomService {

    RoomDto createOrGetIndividualRoom(Long meId, Long targetId);

    GroupRoomResponse createGroupRoom(Long ownerId, CreateGroupRoomRequest request);

    // 사용자가 속한 개인 채팅방 목록 조회
//    List<RoomListResponseDto> findMyIndividualRooms(Long myUserId);

    ChatRoomDetailResponse findChatRoomInfo(Long roomId, Long userId);

    List<RoomListCommonResponseDto> findAllMyRooms(Long userId);

    List<RoomListCommonResponseDto> findMyIndividualRooms(Long userId);

    List<RoomListCommonResponseDto> findMyGroupRooms(Long userId);

    boolean isUserInRoom(Long userId, Long roomId);

}