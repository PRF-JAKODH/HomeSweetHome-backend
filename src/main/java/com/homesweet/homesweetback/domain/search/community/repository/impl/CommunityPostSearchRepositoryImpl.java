package com.homesweet.homesweetback.domain.search.community.repository.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.homesweet.homesweetback.domain.search.community.repository.CommunityPostRepository;
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

import com.homesweet.homesweetback.domain.search.community.repository.document.CommunityPostDocument;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CommunityPostSearchRepositoryImpl implements CommunityPostRepository {

    private final ElasticsearchOperations operations;

    /**
     * 자동완성
     */
    @Override
    public List<String> autocomplete(String keyword) {

        NativeQuery query = NativeQuery.builder()
                .withQuery(buildAutocompleteQuery(keyword))
                .withHighlightQuery(buildHighlightQuery())
                .withPageable(PageRequest.of(0, 10))
                .build();

        SearchHits<CommunityPostDocument> hits =
                operations.search(query, CommunityPostDocument.class);

        return extractAutocompleteResults(hits);
    }

    /**
     * 자동완성 쿼리
     */
    private Query buildAutocompleteQuery(String keyword) {

        return MultiMatchQuery.of(m -> m
                .query(keyword)
                .type(TextQueryType.BoolPrefix)
                .fields(
                        "title.autocomplete",
                        "title.ngram",
                        "title.keyword"
                )
        )._toQuery();
    }

    /**
     * 하이라이트 옵션
     */
    private HighlightQuery buildHighlightQuery() {

        HighlightParameters params = HighlightParameters.builder()
                .withPreTags("<b>")
                .withPostTags("</b>")
                .build();

        Highlight highlight = new Highlight(
                params,
                List.of(new HighlightField("title.autocomplete"))
        );

        return new HighlightQuery(highlight, CommunityPostDocument.class);
    }

    /**
     * 자동완성 결과 추출
     */
    private List<String> extractAutocompleteResults(SearchHits<CommunityPostDocument> hits) {

        List<String> result = new ArrayList<>();

        hits.forEach(hit -> {

            String highlighted = hit.getHighlightField("title.autocomplete")
                    .stream()
                    .findFirst()
                    .orElse(hit.getContent().getTitle()); // fallback

            result.add(highlighted);
        });

        return result;
    }
}