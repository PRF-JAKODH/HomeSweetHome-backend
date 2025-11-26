package com.homesweet.homesweetback.domain.search.chat.controller;

import com.homesweet.homesweetback.common.util.scroll.SearchScrollResponse;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.search.chat.controller.response.ChatRoomSearchResponse;
import com.homesweet.homesweetback.domain.search.chat.controller.response.ChatRoomSortType;
import com.homesweet.homesweetback.domain.search.chat.service.ChatRoomSearchService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 단체 채팅방 검색 컨트롤러
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
@RestController
@RequestMapping("/api/v1/search/chat")
@RequiredArgsConstructor
public class ChatSearchController {

    private final ChatRoomSearchService chatRoomSearchService;

    /**
     * 채팅방 검색어 자동 완성
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocomplete(@NotNull @RequestParam String keyword) {

        List<String> result = chatRoomSearchService.autocomplete(keyword);

        return ResponseEntity.ok(result);
    }

    /**
     * 채팅방 검색 및 조회
     */
    @GetMapping
    public ResponseEntity<SearchScrollResponse<ChatRoomSearchResponse>> searchChatRooms(
            @RequestParam(required = false) String nextCursor,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "LATEST") ChatRoomSortType sortType,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ) {

        Long userId = principal.getUserId();

        SearchScrollResponse<ChatRoomSearchResponse> response =
                chatRoomSearchService.search(userId, nextCursor, keyword, limit, sortType);

        return ResponseEntity.ok(response);
    }
}
