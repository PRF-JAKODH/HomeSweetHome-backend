package com.homesweet.homesweetback.domain.search.chat.service.impl;

import com.homesweet.homesweetback.common.util.CursorUtil;
import com.homesweet.homesweetback.common.util.SearchScrollResponse;
import com.homesweet.homesweetback.domain.product.recent.service.RecentSearchService;
import com.homesweet.homesweetback.domain.search.chat.controller.response.ChatRoomSearchResponse;
import com.homesweet.homesweetback.domain.search.chat.controller.response.ChatRoomSortType;
import com.homesweet.homesweetback.domain.search.chat.repository.ChatRoomSearchRepository;
import com.homesweet.homesweetback.domain.search.chat.repository.document.ChatRoomDocument;
import com.homesweet.homesweetback.domain.search.chat.service.ChatRoomSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 단체 채팅방 검색 서비스 구현체
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomSearchServiceImpl implements ChatRoomSearchService {

    private final ChatRoomSearchRepository chatRoomSearchRepository;
    private final RecentSearchService recentSearchService;
    private final CursorUtil cursorUtil;

    @Override
    public List<String> autocomplete(String keyword) {
        return chatRoomSearchRepository.autocomplete(keyword);
    }

    @Override
    public SearchScrollResponse<ChatRoomSearchResponse> search(Long userId, String cursor, String keyword, int limit, ChatRoomSortType sortType) {

        if (userId != null && keyword != null && !keyword.isBlank()) {
            recentSearchService.save(userId, keyword);
        }

        SearchHits<ChatRoomDocument> hits =
                chatRoomSearchRepository.search(keyword, cursor, limit, sortType);

        List<ChatRoomDocument> docs = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        boolean hasNext = docs.size() > limit;
        List<ChatRoomDocument> result = hasNext ? docs.subList(0, limit) : docs;

        ChatRoomDocument lastDoc = hasNext ? result.getLast() : null;
        Float lastScore = hasNext ? hits.getSearchHits().get(limit - 1).getScore() : null;

        List<Object> sortValues = null;
        if (lastDoc != null) {
            switch (sortType) {

                case RECOMMENDED -> {
                    sortValues = List.of(
                            lastScore != null ? lastScore : 0.0f,
                            lastDoc.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            lastDoc.getChatRoomId()
                    );
                }

                case LATEST -> {
                    sortValues = List.of(
                            lastDoc.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            lastDoc.getChatRoomId()
                    );
                }
            }
        }

        String nextCursor = cursorUtil.encodeSortValues(sortValues);

        List<ChatRoomSearchResponse> responses = result.stream()
                .map(ChatRoomSearchResponse::from)
                .toList();

        return SearchScrollResponse.of(responses, nextCursor, hasNext);
    }
}
