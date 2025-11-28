package com.homesweet.homesweetback.domain.search.chat.repository.impl;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.homesweet.homesweetback.common.util.scroll.ChatRoomCursorStrategy;
import com.homesweet.homesweetback.common.util.scroll.CursorUtil;
import com.homesweet.homesweetback.domain.search.chat.controller.response.ChatRoomSortType;
import com.homesweet.homesweetback.domain.search.chat.repository.ChatRoomSearchRepository;
import com.homesweet.homesweetback.domain.search.chat.repository.document.ChatRoomDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 채팅방 검색 레포지토리 구현체
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.elasticsearch.enabled", havingValue = "true")
public class ChatRoomSearchRepositoryImpl implements ChatRoomSearchRepository {

    private final ElasticsearchOperations operations;
    private final CursorUtil cursorUtil;

    @Override
    public List<String> autocomplete(String keyword) {

        NativeQuery query = NativeQuery.builder()
                .withQuery(buildAutocompleteQuery(keyword))
                .withHighlightQuery(buildHighlightQuery())
                .withPageable(PageRequest.of(0, 10))
                .build();

        SearchHits<ChatRoomDocument> searchHits =
                operations.search(query, ChatRoomDocument.class);

        return extractAutocompleteResults(searchHits);
    }

    /**
     * 검색어 자동 완성용 쿼리
     */
    // 검색어 자동 완성 쿼리
    private Query buildAutocompleteQuery(String keyword) {
        return MultiMatchQuery.of(m -> m
                .query(keyword)
                .type(TextQueryType.BoolPrefix)
                .fields(
                        "chatroom_name.autocomplete",
                        "chatroom_name.ngram",
                        "chatroom_name.keyword"
                )
        )._toQuery();
    }

    // 하이라이트 처리
    private HighlightQuery buildHighlightQuery() {
        HighlightParameters params = HighlightParameters.builder()
                .withPreTags("<b>")
                .withPostTags("</b>")
                .build();

        Highlight highlight = new Highlight(
                params,
                List.of(new HighlightField("chatRoomNameAutocomplete"))
        );

        return new HighlightQuery(highlight, ChatRoomDocument.class);
    }

    // Elastic 검색 실행 쿼리
    private List<String> extractAutocompleteResults(SearchHits<ChatRoomDocument> hits) {

        List<String> result = new ArrayList<>();

        hits.forEach(hit -> {

            String highlighted = hit.getHighlightField("chatRoomNameAutocomplete")
                    .stream()
                    .findFirst()
                    .orElse(hit.getContent().getChatRoomName()); // fallback

            result.add(highlighted);
        });

        return result;
    }
    /**
     * 검색어 자동완성 끝!
     */

    /**
     * 채팅방 검색 (키워드 있을 시 score 기반)
     * 키워드 없을 시 최신순 검색
     */
    @Override
    public SearchHits<ChatRoomDocument> search(String keyword, String nextCursor, int limit, ChatRoomSortType sortType) {

        Query keywordQuery = buildKeywordQuery(keyword);
        Query finalQuery = buildBoolQuery(keywordQuery);
        List<SortOptions> sorts = buildSortOptions(keyword, sortType);

        List<Object> searchAfter = cursorUtil.decode(nextCursor, new ChatRoomCursorStrategy(sortType));
        int fetchSize = limit + 1;

        NativeQuery query = NativeQuery.builder()
                .withQuery(finalQuery)
                .withSort(sorts)
                .withSearchAfter(searchAfter)
                .withPageable(PageRequest.of(0, fetchSize))
                .build();

        return operations.search(query, ChatRoomDocument.class);
    }

    //키워드 여부에 따른 검색 쿼리
    private Query buildKeywordQuery(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return MatchAllQuery.of(m -> m)._toQuery();
        }

        // 자동완성 + 검색 + ngram + fuzzy + 가중치
        return MultiMatchQuery.of(m -> m
                .query(keyword)
                .fields(List.of(
                        "chatroom_name^3",
                        "chatroom_name.ngram^1",
                        "chatroom_name.autocomplete^2"
                ))
                .fuzziness("AUTO")
                .prefixLength(1)
        )._toQuery();
    }

    // 삭제 되지 않는 방 조회
    private Query buildBoolQuery(Query keywordQuery) {

        return BoolQuery.of(b -> b
                .must(keywordQuery)
                .filter(TermQuery.of(t -> t.field("is_deleted").value(false))._toQuery())
        )._toQuery();
    }

    // 정렬 조건
    private List<SortOptions> buildSortOptions(String keyword, ChatRoomSortType sortType) {

        List<SortOptions> sorts = new ArrayList<>();

        switch (sortType) {
            case RECOMMENDED -> {
                if (keyword != null && !keyword.isBlank()) {
                    sorts.add(SortOptions.of(s -> s.field(f -> f.field("_score").order(SortOrder.Desc))));
                }
                sorts.add(SortOptions.of(s -> s.field(f -> f.field("created_at").order(SortOrder.Desc))));
                sorts.add(SortOptions.of(s -> s.field(f -> f.field("chatroom_id").order(SortOrder.Asc))));
            }
            case LATEST -> {
                sorts.add(SortOptions.of(s -> s.field(f -> f.field("created_at").order(SortOrder.Desc))));
                sorts.add(SortOptions.of(s -> s.field(f -> f.field("chatroom_id").order(SortOrder.Asc))));
            }
        }

        return sorts;
    }
}