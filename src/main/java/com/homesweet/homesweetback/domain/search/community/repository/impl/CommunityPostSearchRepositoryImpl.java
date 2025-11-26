package com.homesweet.homesweetback.domain.search.community.repository.impl;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.homesweet.homesweetback.common.util.scroll.CommunityCursorStrategy;
import com.homesweet.homesweetback.common.util.scroll.CursorUtil;
import com.homesweet.homesweetback.domain.search.community.controller.response.CommunitySortType;
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
    private final CursorUtil cursorUtil;

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
                List.of(new HighlightField("titleAutocomplete"))
        );

        return new HighlightQuery(highlight, CommunityPostDocument.class);
    }

    /**
     * 자동완성 결과 추출
     */
    private List<String> extractAutocompleteResults(SearchHits<CommunityPostDocument> hits) {

        List<String> result = new ArrayList<>();

        hits.forEach(hit -> {

            String highlighted = hit.getHighlightField("titleAutocomplete")
                    .stream()
                    .findFirst()
                    .orElse(hit.getContent().getTitle()); // fallback

            result.add(highlighted);
        });

        return result;
    }

    @Override
    public SearchHits<CommunityPostDocument> search(String keyword, String nextCursor, int limit, CommunitySortType sortType) {

        Query keywordQuery = buildKeywordQuery(keyword);
        Query finalQuery = buildBoolQuery(keywordQuery);
        List<SortOptions> sorts = buildSortOptions(keyword, sortType);

        List<Object> searchAfter = cursorUtil.decode(nextCursor, new CommunityCursorStrategy(sortType));
        int fetchSize = limit + 1;

        NativeQuery query = NativeQuery.builder()
                .withQuery(finalQuery)
                .withSort(sorts)
                .withSearchAfter(searchAfter)
                .withPageable(PageRequest.of(0, fetchSize))
                .build();

        return operations.search(query, CommunityPostDocument.class);
    }

    // 검색어 있을 시: 제목 + 내용 MultiMatch
    private Query buildKeywordQuery(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return MatchAllQuery.of(m -> m)._toQuery();
        }

        return MultiMatchQuery.of(m -> m
                .query(keyword)
                .fields(List.of(
                        "title^3",
                        "title.ngram^1",
                        "title.autocomplete^2",
                        "content^1"
                ))
                .fuzziness("AUTO")
                .prefixLength(1)
        )._toQuery();
    }

    // 삭제되지 않은 글만
    private Query buildBoolQuery(Query keywordQuery) {

        return BoolQuery.of(b -> b
                .must(keywordQuery)
                .filter(TermQuery.of(t -> t.field("is_deleted").value(false))._toQuery())
        )._toQuery();
    }

    // 정렬 조건
    private List<SortOptions> buildSortOptions(String keyword, CommunitySortType sortType) {

        List<SortOptions> sorts = new ArrayList<>();

        switch (sortType) {

            case RECOMMENDED -> {
                if (keyword != null && !keyword.isBlank()) {
                    sorts.add(SortOptions.of(s -> s.field(f -> f.field("_score").order(SortOrder.Desc))));
                }
                sorts.add(SortOptions.of(s -> s.field(f -> f.field("created_at").order(SortOrder.Desc))));
                sorts.add(SortOptions.of(s -> s.field(f -> f.field("post_id").order(SortOrder.Asc))));
            }

            case LATEST -> {
                sorts.add(SortOptions.of(s -> s.field(f -> f.field("created_at").order(SortOrder.Desc))));
                sorts.add(SortOptions.of(s -> s.field(f -> f.field("post_id").order(SortOrder.Asc))));
            }

            case VIEW_COUNT -> {
                sorts.add(SortOptions.of(s -> s.field(f -> f.field("view_count").order(SortOrder.Desc))));
                sorts.add(SortOptions.of(s -> s.field(f -> f.field("post_id").order(SortOrder.Asc))));
            }

            case LIKE_COUNT -> {
                sorts.add(SortOptions.of(s -> s.field(f -> f.field("like_count").order(SortOrder.Desc))));
                sorts.add(SortOptions.of(s -> s.field(f -> f.field("post_id").order(SortOrder.Asc))));
            }
        }

        return sorts;
    }
}