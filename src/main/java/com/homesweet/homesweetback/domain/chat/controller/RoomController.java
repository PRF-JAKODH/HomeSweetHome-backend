package com.homesweet.homesweetback.domain.chat.controller;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.chat.dto.request.CreateGroupRoomRequest;
import com.homesweet.homesweetback.domain.chat.dto.request.CreateIndividualRoomRequest;
import com.homesweet.homesweetback.domain.chat.dto.RoomDto;
import com.homesweet.homesweetback.domain.chat.dto.response.*;
import com.homesweet.homesweetback.domain.chat.service.ChatMessageService;
import com.homesweet.homesweetback.domain.chat.service.ChatRoomService;
import com.homesweet.homesweetback.domain.chat.service.RoomMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final RoomMemberService roomMemberService;

    /**
     * 1:1 채팅방 생성 또는 재사용
     * POST /api/v1/chat/rooms/individual
     */
    @PostMapping("/individual")
    public ResponseEntity<RoomDto> createOrGetIndividual(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @Valid @RequestBody CreateIndividualRoomRequest request) {

        Long meId = principal.getUserId();
        Long targetId = request.getTargetId();

        if (meId.equals(targetId)) {
            throw new ResponseStatusException(BAD_REQUEST, "자기 자신과는 1:1 채팅을 만들 수 없습니다.");
        }

        RoomDto response = chatRoomService.createOrGetIndividualRoom(meId, targetId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 그룹채팅방 생성
     * POST /api/v1/chat/rooms/group
     */
    @PostMapping(value = "/group",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GroupRoomCreateResponse> createGroupRoom(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @ModelAttribute @Valid CreateGroupRoomRequest request) {

        Long ownerId = principal.getUserId();
        GroupRoomCreateResponse response = chatRoomService.createGroupRoom(ownerId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 입장 ( 신규 입장, 퇴장했던 멤버가 재입장(is_exit = true))
    @PostMapping("/{roomId}/join")
    public ResponseEntity<Void> joinRoom(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @PathVariable Long roomId
    ) {
        chatRoomService.joinRoom(roomId, principal.getUserId());
        return ResponseEntity.ok().build();
    }

    // 퇴장
    @PostMapping("/{roomId}/exit")
    public ResponseEntity<Void> exitRoom(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @PathVariable Long roomId

    ) {
        Long userId = principal.getUserId();

        chatRoomService.leaveRoom(userId, roomId);
        return ResponseEntity.ok().build();
    }


    // 단순 조회 (채팅방에 속한 멤버가 해당 채팅방을 조회하는 경우)
    @GetMapping("/individual/{roomId}")
    public ResponseEntity<IndividualChatDetailResponse> getIndividualRoomInfo(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @PathVariable Long roomId) {
        Long userId = principal.getUserId();
        IndividualChatDetailResponse response = chatRoomService.getIndividualChatDetail(userId, roomId);

        log.info("responsd: " + response);
        return ResponseEntity.ok(response);
    }

    // TODO: 멤버 목록 업데이트(PUT) 분리
    // 단순 조회 (채팅방에 속한 멤버가 해당 채팅방을 조회하는 경우)
    @GetMapping("/group/{roomId}")
    public ResponseEntity<GroupChatDetailResponse> getGroupRoomInfo(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @PathVariable Long roomId) {

        Long userId = principal.getUserId();
        GroupChatDetailResponse response = chatRoomService.getGroupChatDetail(userId, roomId);

        return ResponseEntity.ok(response);
    }

    // 개인 채팅방 목록 조회
    @GetMapping("/my/individual")
    public ResponseEntity<List<IndividualRoomListResponse>> getMyIndividualRoomList(
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ){
        Long userId = principal.getUserId();
        List<IndividualRoomListResponse> response = chatRoomService.findMyIndividualRooms(userId);
        return ResponseEntity.ok(response);
    }

    // 그룹 채팅방 목록 조회
    @GetMapping("/my/group")
    public ResponseEntity<List<GroupRoomListResponse>> getMyGroupRoomList(
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ){
        Long userId = principal.getUserId();
        List<GroupRoomListResponse> response = chatRoomService.findMyGroupRooms(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 그룹채팅방 전체 조회 (비회원)
     * GET /api/v1/chat/rooms/group/all
     */
    @GetMapping("/group/all")
    public ResponseEntity<List<GroupRoomListResponse>> getAllGroupRooms() {
        List<GroupRoomListResponse> response = chatRoomService.getAllGroupRooms();
        return ResponseEntity.ok(response);
    }

    /**
     * 이전 메세지 목록 조회
     * GET /api/v1/chat/rooms/{roomId}/messages
     */
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<PreMessageResponse> getPreMessageInfo(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long lastMessageId,
            @RequestParam(defaultValue = "30") int size
    ) {
        PreMessageResponse response = chatMessageService.getPreMessage(roomId, lastMessageId, size);
        return ResponseEntity.ok(response);
    }



}
