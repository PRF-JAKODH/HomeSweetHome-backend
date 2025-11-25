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
import com.homesweet.homesweetback.domain.product.product.query.repository.document.ProductDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
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

        HighlightParameters highlightParameters = HighlightParameters.builder()
                .withPreTags("<b>")
                .withPostTags("</b>")
                .build();

        Highlight highlight = new Highlight(
                highlightParameters,
                List.of(new HighlightField("nameAutocomplete"))
        );

        HighlightQuery highlightQuery = new HighlightQuery(highlight, ProductDocument.class);

        Query multiMatchQuery = MultiMatchQuery.of(m -> m
                .query(keyword)
                .type(TextQueryType.BoolPrefix)
                .fields("name.autocomplete", "name.autocomplete._2gram", "name.autocomplete._3gram")
        )._toQuery();

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(multiMatchQuery)
                .withHighlightQuery(highlightQuery)
                .withPageable(PageRequest.of(0, 10))
                .build();

        SearchHits<ProductDocument> searchHits =
                operations.search(nativeQuery, ProductDocument.class);

        return searchHits.getSearchHits().stream()
                .map(hit -> {
                    // highlight 결과가 있으면 highlight 사용
                    List<String> highlights = hit.getHighlightField("nameAutocomplete");

                    if (highlights != null && !highlights.isEmpty()) {
                        return highlights.get(0);  // 하이라이트 버전 반환
                    }

                    return hit.getContent().getName();
                })
                .toList();
    }

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
    public SearchHits<ProductDocument> search(String nextCursor, Long categoryId, int limit, String keyword, ProductSortType sortType, Double minPrice, Double maxPrice) {

        // 1. 키워드 쿼리
        Query keywordQuery = (keyword == null || keyword.isBlank())
                ? Query.of(q -> q.matchAll(m -> m))
                : MultiMatchQuery.of(m -> m
                .query(keyword)
                .fields(List.of("name^3", "category_name^2", "name.ngram", "name.autocomplete", "description"))
                .fuzziness("AUTO")
                .prefixLength(1)
        )._toQuery();

        // 2. 필터들
        List<Query> filters = new ArrayList<>();
        filters.add(TermQuery.of(t -> t.field("status").value("ON_SALE"))._toQuery());

        if (categoryId != null) {
            List<Long> categoryIds = cacheCategory.getAllSubCategoryIds(categoryId);
            filters.add(TermsQuery.of(t -> t
                    .field("category_id")
                    .terms(TermsQueryField.of(tf -> tf.value(categoryIds.stream().map(FieldValue::of).toList())))
            )._toQuery());
        }

        if (minPrice != null || maxPrice != null) {
            Query priceFilter = NumberRangeQuery.of(r -> r
                    .field("base_price")
                    .gte(minPrice)
                    .lte(maxPrice)
            )._toRangeQuery()._toQuery();

            filters.add(priceFilter);
        }

        Query boolQuery = BoolQuery.of(b -> b
                .must(keywordQuery)
                .filter(filters)
        )._toQuery();

        ProductSortType effectiveSortType = (keyword != null && !keyword.isBlank())
                ? ProductSortType.RECOMMENDED
                : sortType;

        List<SortOptions> sorts = buildSortOptions(effectiveSortType, keyword);

        List<Object> searchAfter = cursorUtil.decodeCursor(nextCursor, effectiveSortType);

        int fetchSize = limit + 1;

        NativeQuery query = NativeQuery.builder()
                .withQuery(boolQuery)
                .withSort(sorts)
                .withPageable(PageRequest.of(0, fetchSize))
                .withSearchAfter(searchAfter)
                .build();

        return operations.search(query, ProductDocument.class);
    }

    private List<SortOptions> buildSortOptions(ProductSortType sortType, String keyword) {
        List<SortOptions> sorts = new ArrayList<>();

        // 검색이면 무조건 RECOMMENDED로 강제!
        if (keyword != null && !keyword.isBlank()) {
            sortType = ProductSortType.RECOMMENDED;
        }

        switch (sortType) {
            case RECOMMENDED -> {
                sorts.add(SortOptions.of(s -> s.field(f -> f.field("_score").order(SortOrder.Desc))));
            }
            case LATEST -> sorts.add(SortOptions.of(s -> s.field(f -> f.field("created_at").order(SortOrder.Desc).missing("_last"))));
            case PRICE_LOW -> sorts.add(SortOptions.of(s -> s.field(f -> f.field("base_price").order(SortOrder.Asc).missing("_last"))));
            case PRICE_HIGH -> sorts.add(SortOptions.of(s -> s.field(f -> f.field("base_price").order(SortOrder.Desc).missing("_last"))));
            case POPULAR -> {
                sorts.add(SortOptions.of(s -> s.field(f -> f.field("average_rating").order(SortOrder.Desc).missing("0.0"))));
                sorts.add(SortOptions.of(s -> s.field(f -> f.field("review_count").order(SortOrder.Desc).missing("0"))));
            }
        }

        // tie-breaker 항상 동일 (RECOMMENDED 포함!)
        sorts.add(SortOptions.of(s -> s.field(f -> f.field("product_id").order(SortOrder.Asc))));

        return sorts;
    }
}
