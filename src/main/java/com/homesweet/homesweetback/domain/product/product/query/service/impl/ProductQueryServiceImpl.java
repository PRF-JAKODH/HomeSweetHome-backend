package com.homesweet.homesweetback.domain.product.product.query.service.impl;

import com.homesweet.homesweetback.common.util.CursorUtil;
import com.homesweet.homesweetback.common.util.SearchScrollResponse;
import com.homesweet.homesweetback.domain.product.product.command.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.query.controller.response.ProductPreviewResponse;
import com.homesweet.homesweetback.domain.product.product.query.repository.ProductQueryRepository;
import com.homesweet.homesweetback.domain.product.product.query.repository.document.ProductDocument;
import com.homesweet.homesweetback.domain.product.product.query.service.ProductQueryService;
import com.homesweet.homesweetback.domain.product.recent.service.RecentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 상품 검색 서비스 구현체
 *
 * @author junnukim1007gmail.com
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryServiceImpl implements ProductQueryService {

    private final ProductQueryRepository productQueryRepository;
    private final RecentSearchService recentSearchService;
    private final CursorUtil cursorUtil;


    @Override
    public List<String> autocomplete(String keyword) {
        return productQueryRepository.autocomplete(keyword);
    }

    @Override
    public SearchScrollResponse<ProductPreviewResponse> searchProducts(
            String cursor, Long categoryId, String keyword, ProductSortType sortType,
            Double minPrice, Double maxPrice, int limit, Long userId, List<String> optionFilters) {

        if (userId != null && keyword != null && !keyword.isBlank()) {
            recentSearchService.save(userId, keyword);
        }

        return executeSearch(cursor, categoryId, keyword, sortType, minPrice, maxPrice, limit, optionFilters);
    }

    @Override
    public SearchScrollResponse<ProductPreviewResponse> getProductPreview(String cursor, Long categoryId, String keyword, ProductSortType sortType, Double minPrice, Double maxPrice, int limit, List<String> optionFilters) {
        SearchHits<ProductDocument> hits = productQueryRepository.search(
                cursor, categoryId, limit, keyword, sortType, minPrice, maxPrice, optionFilters);

        return executeSearch(cursor, categoryId, keyword, sortType, minPrice, maxPrice, limit, optionFilters);
    }

    private SearchScrollResponse<ProductPreviewResponse> executeSearch(
            String cursor,
            Long categoryId,
            String keyword,
            ProductSortType sortType,
            Double minPrice,
            Double maxPrice,
            int limit,
            List<String> optionFilters
    ) {

        SearchHits<ProductDocument> hits = productQueryRepository.search(
                cursor, categoryId, limit, keyword, sortType, minPrice, maxPrice, optionFilters);

        List<ProductDocument> docs = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        boolean hasNext = docs.size() > limit;
        List<ProductDocument> result = hasNext ? docs.subList(0, limit) : docs;
        ProductDocument lastDoc = hasNext ? result.getLast() : null;

        Float lastScore = hasNext ? hits.getSearchHits().get(limit - 1).getScore() : null;

        // 공통 cursor 생성 로직도 그대로 유지
        List<Object> sortValues = lastDoc != null ? switch (sortType) {
            case RECOMMENDED -> List.of(lastScore, lastDoc.getProductId());
            case LATEST -> List.of(lastDoc.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    lastDoc.getProductId());
            case PRICE_LOW, PRICE_HIGH ->
                    List.of(lastDoc.getBasePrice(), lastDoc.getProductId());
            case POPULAR -> List.of(
                    lastDoc.getAverageRating() != null ? lastDoc.getAverageRating() : 0.0,
                    lastDoc.getReviewCount() != null ? lastDoc.getReviewCount() : 0,
                    lastDoc.getProductId()
            );
        } : null;

        String nextCursor = cursorUtil.encodeSortValues(sortValues);

        List<ProductPreviewResponse> responses = result.stream()
                .map(ProductPreviewResponse::fromDocument)
                .toList();

        return SearchScrollResponse.of(responses, nextCursor, hasNext);
    }
}
