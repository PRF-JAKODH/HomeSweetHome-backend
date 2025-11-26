package com.homesweet.homesweetback.domain.search.chat.repository.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.homesweet.homesweetback.domain.search.chat.repository.ChatRoomSearchRepository;
import com.homesweet.homesweetback.domain.search.chat.repository.document.ChatRoomDocument;
import lombok.RequiredArgsConstructor;
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
public class ChatRoomSearchRepositoryImpl implements ChatRoomSearchRepository {

    private final ElasticsearchOperations operations;

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

    private Query buildAutocompleteQuery(String keyword) {
        return MultiMatchQuery.of(m -> m
                .query(keyword)
                .type(TextQueryType.BoolPrefix)
                .fields(
                        "chatroom_name",
                        "chatroom_name.ngram",
                        "chatroom_name.keyword"
                )
        )._toQuery();
    }

    private HighlightQuery buildHighlightQuery() {
        HighlightParameters params = HighlightParameters.builder()
                .withPreTags("<b>")
                .withPostTags("</b>")
                .build();

        Highlight highlight = new Highlight(
                params,
                List.of(new HighlightField("chatroom_name"))
        );

        return new HighlightQuery(highlight, ChatRoomDocument.class);
    }

    private List<String> extractAutocompleteResults(SearchHits<ChatRoomDocument> hits) {

        List<String> result = new ArrayList<>();

        hits.forEach(hit -> {

            String highlighted = hit.getHighlightField("chatroom_name")
                    .stream()
                    .findFirst()
                    .orElse(hit.getContent().getChatRoomName()); // fallback

            result.add(highlighted);
        });

        return result;
    }
}
