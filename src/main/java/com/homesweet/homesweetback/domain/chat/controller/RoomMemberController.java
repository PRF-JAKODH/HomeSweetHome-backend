package com.homesweet.homesweetback.domain.chat.controller;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.chat.dto.response.RegisterGroupMemberResponse;
import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
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
    private final RoomMemberRepository roomMemberRepository;



    // 새 멤버 등록
//    @PostMapping("/group/{roomId}")
//    public ResponseEntity<RegisterGroupMemberResponse> registerGroupRoom(
//            @AuthenticationPrincipal OAuth2UserPrincipal principal,
//            @PathVariable Long roomId) {
//
//        Long userId = principal.getUserId();
//
//        RegisterGroupMemberResponse response = roomMemberService.registerGroupMember(roomId, userId);
//
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }


    // 임시 멤버 목록 갱신용 컨트롤러
    @PatchMapping("/{roomId}/members/refresh")
    public ResponseEntity<Void> refreshGroupMembers(

            @PathVariable Long roomId) {


        return ResponseEntity.ok().build();
    }


}
