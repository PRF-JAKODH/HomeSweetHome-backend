package com.homesweet.homesweetback.domain.chat.controller;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.chat.dto.request.CreateGroupRoomRequest;
import com.homesweet.homesweetback.domain.chat.dto.request.CreateIndividualRoomRequest;
import com.homesweet.homesweetback.domain.chat.dto.RoomDto;
import com.homesweet.homesweetback.domain.chat.dto.response.*;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import com.homesweet.homesweetback.domain.chat.service.ChatMessageService;
import com.homesweet.homesweetback.domain.chat.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.homesweet.homesweetback.domain.chat.entity.QChatRoom.chatRoom;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

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


    // 개인채팅방 상세조회
    @GetMapping("/individual/{roomId}")
    public ResponseEntity<IndividualChatDetailResponse> getIndividualRoomInfo(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @PathVariable Long roomId) {
        Long userId = principal.getUserId();
        IndividualChatDetailResponse response = chatRoomService.getIndividualChatDetail(userId, roomId);

        log.info("responsd: " + response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/group/{roomId}")
    public ResponseEntity<GroupChatDetailResponse> getGroupRoomInfo(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @PathVariable Long roomId) {

        Long userId = principal.getUserId();
        GroupChatDetailResponse response = chatRoomService.getGroupChatDetail(userId, roomId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my/individual")
    public ResponseEntity<List<IndividualRoomListResponse>> getMyIndividualRoomList(
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ){
        Long userId = principal.getUserId();
        List<IndividualRoomListResponse> response = chatRoomService.findMyIndividualRooms(userId);
        return ResponseEntity.ok(response);
    }

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

//    // ??
//    @PostMapping("/{roomId}/join")
//    public ResponseEntity<Void> joinRoom(
//            @AuthenticationPrincipal OAuth2UserPrincipal principal,
//            @PathVariable Long roomId
//    ) {
//        chatRoomService.joinRoom(roomId, principal.getUserId());
//        return ResponseEntity.ok().build();
//    }

    // 퇴장
    @PostMapping("/{roomId}/exit")
    public ResponseEntity<Void> exitRoom(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @PathVariable Long roomId

    ) {
        Long userId = principal.getUserId();

        chatRoomService.exitRoom(userId, roomId);
        return ResponseEntity.ok().build();
    }

}
