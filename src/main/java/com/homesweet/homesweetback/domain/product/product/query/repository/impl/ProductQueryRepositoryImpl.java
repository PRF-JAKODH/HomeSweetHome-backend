package com.homesweet.homesweetback.domain.product.product.query.repository.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.ScriptSortType;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.homesweetback.common.util.CursorUtil;
import com.homesweet.homesweetback.domain.product.category.service.cache.CacheCategory;
import com.homesweet.homesweetback.domain.product.product.command.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.query.repository.ProductQueryRepository;
import com.homesweet.homesweetback.domain.product.product.query.repository.document.ProductDocument;
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
 * 상품 검색 레포 구현체
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 24.
 */
@Repository
@RequiredArgsConstructor
public class ProductQueryRepositoryImpl implements ProductQueryRepository {

    private final ElasticsearchOperations operations;
    private final CacheCategory cacheCategory;
    private final CursorUtil cursorUtil;

    /**
     * 검색어 자동 완성
     */
    @Override
    public List<String> autocomplete(String keyword) {

        NativeQuery query = NativeQuery.builder()
                .withQuery(buildAutocompleteQuery(keyword))
                .withHighlightQuery(buildHighlightQuery())
                .withPageable(PageRequest.of(0, 10))
                .build();

        SearchHits<ProductDocument> searchHits =
                operations.search(query, ProductDocument.class);

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
                .fields("name.autocomplete", "name.autocomplete._2gram", "name.autocomplete._3gram")
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
                List.of(new HighlightField("nameAutocomplete"))
        );

        return new HighlightQuery(highlight, ProductDocument.class);
    }

    // Elastic 검색 실행 쿼리
    private List<String> extractAutocompleteResults(SearchHits<ProductDocument> searchHits) {
        return searchHits.getSearchHits().stream()
                .map(hit -> {
                    List<String> highlights = hit.getHighlightField("nameAutocomplete");
                    if (!highlights.isEmpty()) {
                        return highlights.getFirst();
                    }
                    return hit.getContent().getName();
                })
                .toList();
    }
    /**
     * 끝!
     */

    /**
     * 상품 조회 및 검색
     *
     * @param nextCursor
     * @param categoryId
     * @param limit
     * @param keyword    검색용 키워드
     * @param sortType   정렬 방법 (인기순, 최저가, 최고가, 최신순)
     * @param minPrice   최저 가격
     * @param maxPrice   최고 가격
     * @return
     */
    @Override
    public SearchHits<ProductDocument> search(String nextCursor, Long categoryId, int limit, String keyword, ProductSortType sortType, Double minPrice, Double maxPrice, List<String> optionFilters) {

        Query keywordQuery = buildKeywordQuery(keyword);

        List<Query> filters = buildFilterQueries(categoryId, minPrice, maxPrice);

        List<Query> optionShouldQueries = buildOptionShouldQueries(optionFilters);

        List<Query> shouldQueries = buildShouldQueries(keywordQuery, optionShouldQueries, keyword);

        Query boolQuery = buildBoolQuery(filters, shouldQueries);

        List<SortOptions> sorts = buildSortOptions(sortType);

        List<Object> searchAfter = cursorUtil.decodeCursor(nextCursor, sortType);

        int fetchSize = limit + 1;

        NativeQuery query = NativeQuery.builder()
                .withQuery(boolQuery)
                .withSort(sorts)
                .withPageable(PageRequest.of(0, fetchSize))
                .withSearchAfter(searchAfter)
                .build();

        return operations.search(query, ProductDocument.class);

    }

    /**
     * 다중 쿼리 (상품 조회 및 검색에서 사용)
     */
    // 키워드 검색 쿼리
    private Query buildKeywordQuery(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return MatchAllQuery.of(m -> m)._toQuery();
        }

        return MultiMatchQuery.of(m -> m
                .query(keyword)
                .fields(List.of("name^3","category_name^2","name.ngram","name.autocomplete","description"))
                .fuzziness("AUTO")
                .prefixLength(1)
        )._toQuery();
    }

    // 필터 쿼리 (가격, 카테고리)
    private List<Query> buildFilterQueries(Long categoryId, Double minPrice, Double maxPrice) {
        List<Query> filters = new ArrayList<>();
        filters.add(TermQuery.of(t -> t.field("status").value("ON_SALE"))._toQuery());

        if (categoryId != null) {
            List<Long> categoryIds = cacheCategory.getAllSubCategoryIds(categoryId);

            filters.add(
                    TermsQuery.of(t -> t
                            .field("category_id")
                            .terms(tf -> tf.value(categoryIds.stream().map(FieldValue::of).toList()))
                    )._toQuery()
            );
        }

        if (minPrice != null || maxPrice != null) {
            NumberRangeQuery.Builder pb = new NumberRangeQuery.Builder().field("sale_price");
            if (minPrice != null) pb.gte(minPrice);
            if (maxPrice != null) pb.lte(maxPrice);
            filters.add(pb.build()._toRangeQuery()._toQuery());
        }

        return filters;
    }

    // 옵션 필터링 쿼리
    private List<Query> buildOptionShouldQueries(List<String> optionFilters) {

        List<Query> shouldQueries = new ArrayList<>();
        if (optionFilters == null) return shouldQueries;

        for (String opt : optionFilters) {
            String[] parts = opt.split(":");
            if (parts.length != 2) continue;

            String group = parts[0];
            String value = parts[1];

            // Nested 옵션 일치
            Query nestedQuery = NestedQuery.of(n -> n
                    .path("option_groups")
                    .query(
                            BoolQuery.of(b -> b.must(List.of(
                                    TermQuery.of(t -> t.field("option_groups.group_name").value(group))._toQuery(),
                                    TermQuery.of(t -> t.field("option_groups.values").value(value))._toQuery()
                            )))
                    )
            )._toQuery();

            shouldQueries.add(nestedQuery);

            // 옵션 값이 이름(name)에 포함
            shouldQueries.add(
                    MatchQuery.of(m -> m
                            .field("name")
                            .query(value)
                            .fuzziness("AUTO")
                    )._toQuery()
            );
        }

        return shouldQueries;
    }

    private List<Query> buildShouldQueries(Query keywordQuery, List<Query> optionQueries, String keyword) {
        List<Query> shouldQueries = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            shouldQueries.add(keywordQuery);
        }

        shouldQueries.addAll(optionQueries);

        return shouldQueries;
    }

    private Query buildBoolQuery(List<Query> filters, List<Query> shouldQueries) {
        return BoolQuery.of(b -> b
                .filter(filters)
                .should(shouldQueries)
                .minimumShouldMatch(shouldQueries.isEmpty() ? "0" : "1")
        )._toQuery();
    }

    // 정렬 기준 조건
    private List<SortOptions> buildSortOptions(ProductSortType sortType) {
        List<SortOptions> sorts = new ArrayList<>();

        switch (sortType) {
            case RECOMMENDED -> {
                sorts.add(SortOptions.of(s -> s.field(f -> f.field("_score").order(SortOrder.Desc))));
            }
            case LATEST -> sorts.add(SortOptions.of(s -> s.field(f -> f.field("created_at").order(SortOrder.Desc).missing("_last"))));
            case PRICE_LOW -> sorts.add(SortOptions.of(s -> s.field(f -> f.field("sale_price").order(SortOrder.Asc).missing("_last"))));
            case PRICE_HIGH -> sorts.add(SortOptions.of(s -> s.field(f -> f.field("sale_price").order(SortOrder.Desc).missing("_last"))));
            case POPULAR -> {
                sorts.add(SortOptions.of(s -> s.field(f -> f.field("average_rating").order(SortOrder.Desc).missing("0.0"))));
                sorts.add(SortOptions.of(s -> s.field(f -> f.field("review_count").order(SortOrder.Desc).missing("0"))));
            }
        }

        sorts.add(SortOptions.of(s -> s.field(f -> f.field("product_id").order(SortOrder.Asc))));

        return sorts;
    }
    /**
     *  끝!
     */
}
