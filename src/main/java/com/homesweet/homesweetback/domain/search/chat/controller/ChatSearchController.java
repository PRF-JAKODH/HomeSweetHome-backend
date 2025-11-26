package com.homesweet.homesweetback.domain.search.chat.controller;

import com.homesweet.homesweetback.domain.search.chat.service.ChatRoomSearchService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocomplete(@NotNull @RequestParam String keyword) {

        List<String> result = chatRoomSearchService.autocomplete(keyword);

        return ResponseEntity.ok(result);
    }
}
