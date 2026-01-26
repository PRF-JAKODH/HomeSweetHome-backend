package com.homesweet.homesweetback.domain.chat.controller;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.chat.dto.response.JoinRoomResponse;
import com.homesweet.homesweetback.domain.chat.service.ChatRoomService;
import com.homesweet.homesweetback.domain.chat.service.RoomMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat/members")
@RequiredArgsConstructor
public class RoomMemberController {

    private final RoomMemberService roomMemberService;
    private final ChatRoomService chatRoomService;


    // TODO 1 : 신규 멤버 및 퇴장한 멤버 등록
    // 신규 멤버 등록
    @PostMapping("/group/{roomId}")
    public ResponseEntity<JoinRoomResponse> createOrRejoinMember(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @PathVariable Long roomId) {

        Long userId = principal.getUserId();
        JoinRoomResponse response = chatRoomService.joinRoom(roomId, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // TODO 2 : 멤버 목록 조회


    // TODO 3 : 멤버 목록 업데이트



    // 임시 멤버 목록 갱신용 컨트롤러
    @PatchMapping("/{roomId}/members/refresh")
    public ResponseEntity<Void> refreshGroupMembers(

            @PathVariable Long roomId) {


        return ResponseEntity.ok().build();
    }


}
