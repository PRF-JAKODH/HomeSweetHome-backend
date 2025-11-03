package com.homesweet.homesweetback.domain.chat.controller;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.chat.dto.request.CreateIndividualRoomRequest;
import com.homesweet.homesweetback.domain.chat.dto.RoomDto;
import com.homesweet.homesweetback.domain.chat.dto.response.ChatRoomDetailResponse;
import com.homesweet.homesweetback.domain.chat.dto.response.PreMessageResponse;
import com.homesweet.homesweetback.domain.chat.dto.response.RoomEnterResponse;
import com.homesweet.homesweetback.domain.chat.dto.response.RoomListResponseDto;
import com.homesweet.homesweetback.domain.chat.service.ChatMessageService;
import com.homesweet.homesweetback.domain.chat.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    /**
     * 1:1 채팅방 생성 또는 재사용
     * POST /api/v1/chat/rooms/individual
     */
    @PostMapping("/individual")
    public RoomDto createOrGetIndividual(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @Valid @RequestBody CreateIndividualRoomRequest req) {

        log.debug("채팅방 생성 or 재사용 test" + req);

        Long meId = principal.getUserId();
        Long targetId = req.getTargetId();

        return chatRoomService.createOrGetIndividualRoom(meId, targetId);
    }

    /**
     * 내가 속한 1:1 채팅방 list 조회
     */
    @GetMapping("/individual")
    public ResponseEntity<List<RoomListResponseDto>> getMyIndividualRooms(
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ) {
        Long myUserId = principal.getUserId();

        List<RoomListResponseDto> roomList = chatRoomService.findMyIndividualRooms(myUserId);

        return ResponseEntity.ok(roomList);
    }

    /**
     *  채팅방 상세 조회
     */
    @GetMapping("/{roomId}")
    public ChatRoomDetailResponse getChatRoomInfo(
            @PathVariable Long roomId,
            @AuthenticationPrincipal OAuth2UserPrincipal principal) {

        Long userId = principal.getUserId();

        log.info("채팅방 정보 조회 요청 - 방 ID: {}, 사용자 ID: {}, 사용자명: {}",
                roomId, userId, principal.getName());

        return chatRoomService.findChatRoomInfo(roomId, userId);
    }


    /**
     *  이전 메세지 목록 조회
     */

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<PreMessageResponse> getPreMessageInfo(
            @PathVariable Long roomId,
            @RequestParam (required = false) Long lastMessageId,
            @RequestParam(defaultValue = "30") int size
    ) {
        PreMessageResponse response = chatMessageService.getPreMessage(roomId, lastMessageId, size);
        return ResponseEntity.ok(response);
    }


    /**
     * 방 입장 시 방 정보 + 메시지 동시 조회
     */
    @GetMapping("/{roomId}/enter")
    public ResponseEntity<RoomEnterResponse> enterRoom(
            @PathVariable Long roomId,

            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ){
        Long userId = principal.getUserId();

        log.debug("enterRoom 요청 " + roomId + "," + userId);

        ChatRoomDetailResponse room = chatRoomService.findChatRoomInfo(roomId, userId);
        PreMessageResponse preMessageResponse = chatMessageService.getPreMessage(roomId, null, 30 );

        return ResponseEntity.ok(RoomEnterResponse.of(room, preMessageResponse));
        }

    }





