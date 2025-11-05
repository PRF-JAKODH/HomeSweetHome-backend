package com.homesweet.homesweetback.domain.chat.controller;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.chat.dto.request.CreateGroupRoomRequest;
import com.homesweet.homesweetback.domain.chat.dto.request.CreateIndividualRoomRequest;
import com.homesweet.homesweetback.domain.chat.dto.RoomDto;
import com.homesweet.homesweetback.domain.chat.dto.response.*;
import com.homesweet.homesweetback.domain.chat.service.ChatMessageService;
import com.homesweet.homesweetback.domain.chat.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
            @Valid @RequestBody CreateIndividualRoomRequest request) {

        log.debug("채팅방 생성 or 재사용 test" + request);

        Long meId = principal.getUserId();
        Long targetId = request.getTargetId();

        return chatRoomService.createOrGetIndividualRoom(meId, targetId);
    }

    /**
     *  채팅방 상세 조회
     * GET /api/v1/chat/rooms/{roomId}
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
     *  GET /api/v1/chat/rooms/{roomId}/messages
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
     * GET /api/v1/chat/rooms/{roomId}/enter
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

    /**
     * 그룹채팅방 생성
     * POST /api/v1/chat/rooms/group
     */
    @PostMapping("/group")
    public ResponseEntity<GroupRoomResponse> createGroupRoom(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @Valid @RequestBody CreateGroupRoomRequest request) {

        Long ownerId = principal.getUserId();

        GroupRoomResponse response = chatRoomService.createGroupRoom(ownerId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내가 속한 모든 채팅방 목록 조회 (개인 + 그룹)
     */
    @GetMapping("/my")
    public ResponseEntity<List<RoomListCommonResponseDto>> getAllMyRooms(
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ) {
        Long userId = principal.getUserId();
        List<RoomListCommonResponseDto> response = chatRoomService.findAllMyRooms(userId);

        //        List<RoomListResponseDto> roomList = chatRoomService.findMyIndividualRooms(myUserId);

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ 내가 속한 1:1 채팅방 목록 조회
     */
    @GetMapping("/my/individual")
    public ResponseEntity<List<RoomListCommonResponseDto>> getMyIndividualRooms(
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ) {
        Long userId = principal.getUserId();
        List<RoomListCommonResponseDto> response = chatRoomService.findMyIndividualRooms(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ 내가 속한 그룹 채팅방 목록 조회
     */
    @GetMapping("/my/group")
    public ResponseEntity<List<RoomListCommonResponseDto>> getMyGroupRooms(
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ) {
        Long userId = principal.getUserId();
        List<RoomListCommonResponseDto> response = chatRoomService.findMyGroupRooms(userId);
        return ResponseEntity.ok(response);
    }


}


