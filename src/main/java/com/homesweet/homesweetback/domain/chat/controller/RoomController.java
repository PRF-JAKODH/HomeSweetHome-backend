package com.homesweet.homesweetback.domain.chat.controller;


import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.chat.dto.request.CreateIndividualRoomRequest;
import com.homesweet.homesweetback.domain.chat.dto.RoomDto;
import com.homesweet.homesweetback.domain.chat.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final ChatRoomService chatRoomService;

    /**
     * 1:1 채팅방 생성 또는 재사용
     * POST /api/chat/rooms/individual
     */
    @PostMapping("/individual")
    @ResponseStatus(HttpStatus.OK)
    public RoomDto createOrGetIndividual(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @Valid @RequestBody CreateIndividualRoomRequest req) {

        Long meId = principal.getUserId();
        Long targetId = req.getTargetId();

        return chatRoomService.createOrGetIndividualRoom(meId, targetId);
    }



//    /**
//     * 내가 속한 1:1 방 ID List 조회
//     * GET /api/chat/rooms/individual
//     */
    @GetMapping("/individual")
    public ResponseEntity<List<Long>> getMyIndividualRoomIds(
            @AuthenticationPrincipal OAuth2UserPrincipal principal) {

        Long meUserId = principal.getUserId();

        List<Long> roomIds = chatRoomService.findMyIndividualRoomIds(meUserId);

        return ResponseEntity.ok(roomIds);
    }



//    /**
//     * 내가 속한 1:1 방의 상세 정보 List 조회
//     * GET /api/chat/rooms/individual
//     */
//    @GetMapping("/individual")
//    public ResponseEntity<Page<RoomListDto>> getIndividualRooms(
//            @RequestParam ("me") Long meUserId,
//            @RequestParam(defaultValue = "0") @Min(0) int page,
//            @RequestParam(defaultValue = "20") @Min(1) int size
//    ) {
//        int safeSize = Math.min(size, 100);
//        return ResponseEntity.ok(
//                chatRoomService.listIndividualRooms(meUserId, page, size)
//        );

}


//인증 사용 meId는 SecurityContext(예: @AuthenticationPrincipal)에서 꺼내고, 요청에선 targetId만 받는 형태
//    private final Map<String, Long> pairToRoom = new ConcurrentHashMap<>();
//    private final SimpMessagingTemplate template; // 보내는 객체
//    private final AtomicLong seq = new AtomicLong(1);
//
//    private String keyOf(long a, long b) {
//        return (a < b) ? (a + ":" + b) : (b + ":" + a);
//    }
//
//    @GetMapping("/individual")
//    public Map<String, Long> createOrGet(@RequestParam Long me,@RequestParam Long targetId) {
//        String key = keyOf(me, targetId);
//        Long roomId = pairToRoom.get(key);
//
//        if (roomId == null) {
//            roomId = seq.getAndIncrement();
//            pairToRoom.put(key, roomId);
//        }
//        return Map.of("roomId", roomId);
//    }

