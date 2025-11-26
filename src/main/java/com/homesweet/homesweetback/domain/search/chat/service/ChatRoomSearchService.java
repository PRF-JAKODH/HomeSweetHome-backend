package com.homesweet.homesweetback.domain.search.chat.service;

import com.homesweet.homesweetback.common.util.scroll.SearchScrollResponse;
import com.homesweet.homesweetback.domain.search.chat.controller.response.ChatRoomSearchResponse;
import com.homesweet.homesweetback.domain.search.chat.controller.response.ChatRoomSortType;

import java.util.List;

/**
 * 단체 채팅방 검색 서비스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
public interface ChatRoomSearchService {

    List<String> autocomplete(String keyword);

    SearchScrollResponse<ChatRoomSearchResponse> search(Long userId, String cursor, String keyword, int limit, ChatRoomSortType sortType);

}
