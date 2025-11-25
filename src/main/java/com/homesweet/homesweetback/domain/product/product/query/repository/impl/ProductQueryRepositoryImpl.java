package com.homesweet.homesweetback.domain.product.product.query.repository.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.ScriptSortType;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;
import com.homesweet.homesweetback.domain.product.category.service.cache.CacheCategory;
import com.homesweet.homesweetback.domain.product.product.command.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.query.repository.ProductQueryRepository;
import com.homesweet.homesweetback.domain.product.product.query.repository.document.ProductDocument;
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
     * @param cursorId
     * @param categoryId
     * @param limit
     * @param keyword    검색용 키워드
     * @param sortType   정렬 방법 (인기순, 최저가, 최고가, 최신순)
     * @param minPrice   최저 가격
     * @param maxPrice   최고 가격
     * @return
     */
    @Override
    public List<ProductDocument> search(Long cursorId, Long categoryId, int limit, String keyword, ProductSortType sortType, Double minPrice, Double maxPrice) {

        /* -----------------------------
         * 1) multi_match / match_all
         * --------------------------- */
        Query keywordQuery;

        if (keyword == null || keyword.isBlank()) {
            keywordQuery = Query.of(q -> q.matchAll(m -> m));
        } else {
            keywordQuery = MultiMatchQuery.of(m -> m
                    .query(keyword)
                    .fields("name^3")
                    .fields("category_name^2")
                    .fields("name.ngram")
                    .fields("name.autocomplete")
                    .fields("description")
                    .fuzziness("AUTO")
                    .prefixLength(1)
            )._toQuery();
        }

        /* -----------------------------
         * 2) filters
         * --------------------------- */
        List<Query> filters = new ArrayList<>();

        // 상태 = ON_SALE
        filters.add(
                TermQuery.of(t -> t
                        .field("status")
                        .value(FieldValue.of("ON_SALE"))
                )._toQuery()
        );

        // 카테고리 + 하위 카테고리
        if (categoryId != null) {
            List<Long> categoryIds = cacheCategory.getAllSubCategoryIds(categoryId);

            List<FieldValue> vals = categoryIds.stream()
                    .map(FieldValue::of)
                    .toList();

            filters.add(Query.of(q -> q
                    .terms(t -> t.field("category_id").terms(v -> v.value(vals)))
            ));
        }

        // 가격 필터
        if (minPrice != null || maxPrice != null) {
            Query priceFilter = NumberRangeQuery.of(r -> r
                    .field("base_price")
                    .gte(minPrice)
                    .lte(maxPrice)
            )._toRangeQuery()._toQuery();

            filters.add(priceFilter);
        }

        /* -----------------------------
         * 3) BoolQuery
         * --------------------------- */
        Query finalQuery = BoolQuery.of(b -> b
                .must(keywordQuery)
                .filter(filters)
        )._toQuery();

        /* -----------------------------
         * 4) 정렬 기준 정의
         * --------------------------- */
        List<SortOptions> sorts = new ArrayList<>();

        switch (sortType) {
            case LATEST -> sorts.add(SortOptions.of(
                    s -> s.field(f -> f.field("created_at").order(SortOrder.Desc))
            ));

            case PRICE_LOW -> sorts.add(SortOptions.of(
                    s -> s.field(f -> f.field("base_price").order(SortOrder.Asc))
            ));

            case PRICE_HIGH -> sorts.add(SortOptions.of(
                    s -> s.field(f -> f.field("base_price").order(SortOrder.Desc))
            ));

            case POPULAR -> {
                // 평점 높은 순
                sorts.add(SortOptions.of(s -> s.field(f -> f
                        .field("average_rating")
                        .order(SortOrder.Desc)
                        .missing("_last")
                )));
                // 리뷰 많은 순
                sorts.add(SortOptions.of(s -> s.field(f -> f
                        .field("review_count")
                        .order(SortOrder.Desc)
                        .missing("_last")
                )));
            }
        }

        // 마지막 tie-breaker 정렬
        sorts.add(SortOptions.of(
                s -> s.field(f -> f.field("product_id").order(SortOrder.Asc))
        ));

        /* -----------------------------
         * 5) NativeQuery 실행 (첫 페이지)
         * --------------------------- */
        NativeQuery firstQuery = NativeQuery.builder()
                .withQuery(finalQuery)
                .withSort(sorts)
                .withPageable(PageRequest.of(0, limit + 1))
                .build();

        SearchHits<ProductDocument> hits =
                operations.search(firstQuery, ProductDocument.class);

        // 첫 페이지면 바로 리턴
        if (cursorId == null) {
            return hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .toList();
        }

        /* -----------------------------
         * 6) search_after 값 구성
         * --------------------------- */

        ProductDocument last = hits.getSearchHits()
                .get(hits.getSearchHits().size() - 1)
                .getContent();

        List<Object> searchAfter = new ArrayList<>();

        switch (sortType) {

            case LATEST -> {
                searchAfter.add(last.getCreatedAt());
                searchAfter.add(last.getProductId());
            }

            case PRICE_LOW, PRICE_HIGH -> {
                searchAfter.add(last.getBasePrice());
                searchAfter.add(last.getProductId());
            }

            case POPULAR -> {
                searchAfter.add(last.getAverageRating());
                searchAfter.add(last.getReviewCount());
                searchAfter.add(last.getProductId());
            }
        }

        /* -----------------------------
         * 7) search_after 기반 재요청
         * --------------------------- */
        NativeQuery nextQuery = NativeQuery.builder()
                .withQuery(finalQuery)
                .withSort(sorts)
                .withSearchAfter(searchAfter)
                .withPageable(PageRequest.of(0, limit + 1))
                .build();

        SearchHits<ProductDocument> nextHits =
                operations.search(nextQuery, ProductDocument.class);

        return nextHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
    }
}
