package com.homesweet.homesweetback.domain.search.chat.repository;

import com.homesweet.homesweetback.domain.search.chat.controller.response.ChatRoomSortType;
import com.homesweet.homesweetback.domain.search.chat.repository.document.ChatRoomDocument;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.List;

/**
 * 채팅방 검색 레포지토리
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
public interface ChatRoomSearchRepository {

    List<String> autocomplete(String keyword);

    SearchHits<ChatRoomDocument> search(String keyword, String nextCursor, int limit, ChatRoomSortType sortType);
}
