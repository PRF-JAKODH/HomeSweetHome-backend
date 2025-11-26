package com.homesweet.homesweetback.domain.search.chat.controller;

import com.homesweet.homesweetback.domain.search.chat.service.ChatSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private final ChatSearchService chatSearchService;

}
