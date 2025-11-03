package com.homesweet.homesweetback.domain.chat.service;

import com.homesweet.homesweetback.domain.chat.dto.RoomDto;
import com.homesweet.homesweetback.domain.chat.dto.response.ChatRoomDetailResponse;
import com.homesweet.homesweetback.domain.chat.dto.response.RoomListResponseDto;

import java.util.List;

public interface ChatRoomService {

    RoomDto createOrGetIndividualRoom(Long meId, Long targetId);

    /**
     * 내가 속한 1:1 채팅방 목록 조회
     */
    List<RoomListResponseDto> findMyIndividualRooms(Long myUserId);


    ChatRoomDetailResponse findChatRoomInfo(Long roomId, Long userId);

}