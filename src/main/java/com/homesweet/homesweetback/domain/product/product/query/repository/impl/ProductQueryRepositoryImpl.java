package com.homesweet.homesweetback.domain.product.product.query.repository.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
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
}
