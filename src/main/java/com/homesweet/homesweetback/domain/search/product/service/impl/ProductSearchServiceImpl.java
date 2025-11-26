package com.homesweet.homesweetback.domain.search.product.service.impl;

import com.homesweet.homesweetback.common.util.CursorUtil;
import com.homesweet.homesweetback.common.util.SearchScrollResponse;
import com.homesweet.homesweetback.common.valid.ProductValidator;
import com.homesweet.homesweetback.domain.product.product.command.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.ProductDetailResponse;
import com.homesweet.homesweetback.domain.product.product.command.repository.ProductRepository;
import com.homesweet.homesweetback.domain.search.product.controller.response.ProductPreviewResponse;
import com.homesweet.homesweetback.domain.search.product.repository.ProductSearchRepository;
import com.homesweet.homesweetback.domain.search.product.repository.document.ProductDocument;
import com.homesweet.homesweetback.domain.product.recent.service.RecentSearchService;
import com.homesweet.homesweetback.domain.product.recent.service.RecentViewService;
import com.homesweet.homesweetback.domain.search.product.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ProductSearchRepository productSearchRepository;
    private final RecentSearchService recentSearchService;
    private final CursorUtil cursorUtil;
    private final ProductValidator productValidator;
    private final ProductRepository productRepository;
    private final RecentViewService recentViewService;


    @Override
    public List<String> autocomplete(String keyword) {
        return productSearchRepository.autocomplete(keyword);
    }

    /**
     * [인증] 사용자 상품 검색 및 조회
     */
    @Override
    public SearchScrollResponse<ProductPreviewResponse> searchProducts(
            String cursor, Long categoryId, String keyword, ProductSortType sortType,
            Double minPrice, Double maxPrice, int limit, Long userId, List<String> optionFilters) {

        if (userId != null && keyword != null && !keyword.isBlank()) {
            recentSearchService.save(userId, keyword);
        }

        return executeSearch(cursor, categoryId, keyword, sortType, minPrice, maxPrice, limit, optionFilters);
    }

    /**
     * [비인증] 사용자 상품 검색 및 조회
     */
    @Override
    public SearchScrollResponse<ProductPreviewResponse> getProductPreview(String cursor, Long categoryId, String keyword, ProductSortType sortType, Double minPrice, Double maxPrice, int limit, List<String> optionFilters) {

        return executeSearch(cursor, categoryId, keyword, sortType, minPrice, maxPrice, limit, optionFilters);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long userId, Long productId) {

        productValidator.validateExistsProduct(productId);

        // DB 상세 조회
        ProductDetailResponse detail = productRepository.findProductDetailById(productId);

        // productId 저장
        recentViewService.saveView(userId, productId);

        recentViewService.cacheDetail(productId, detail);

        return detail;
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

        SearchHits<ProductDocument> hits = productSearchRepository.search(
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
