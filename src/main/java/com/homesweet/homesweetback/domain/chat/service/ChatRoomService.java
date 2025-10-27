package com.homesweet.homesweetback.domain.chat.service;

import com.homesweet.homesweetback.domain.chat.dto.RoomDto;
import com.homesweet.homesweetback.domain.chat.dto.RoomListDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatRoomService {

    RoomDto createOrGetIndividualRoom(Long meId, Long targetId);

    List<Long> findMyIndividualRoomIds(Long meUserId);

    //  Page<RoomListDto> listIndividualRooms(Long meId, int page, int size);


}