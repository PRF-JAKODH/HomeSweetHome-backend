package com.homesweet.homesweetback.domain.product.product.query.service.impl;

import com.homesweet.homesweetback.common.util.ScrollResponse;
import com.homesweet.homesweetback.domain.product.product.command.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.ProductPreviewResponse;
import com.homesweet.homesweetback.domain.product.product.query.repository.ProductQueryRepository;
import com.homesweet.homesweetback.domain.product.product.query.repository.document.ProductDocument;
import com.homesweet.homesweetback.domain.product.product.query.service.ProductQueryService;
import com.homesweet.homesweetback.domain.product.recent.service.RecentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 상품 검색 서비스 구현체
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 24.
 */
@Service
@RequiredArgsConstructor
public class ProductQueryServiceImpl implements ProductQueryService {

    private final ProductQueryRepository productQueryRepository;
    private final RecentSearchService recentSearchService;


    @Override
    public List<String> autocomplete(String keyword) {
        return productQueryRepository.autocomplete(keyword);
    }

    @Override
    public ScrollResponse<ProductPreviewResponse> searchProducts(Long cursorId, Long categoryId, String keyword, ProductSortType sortType, Double minPrice, Double maxPrice, int limit, Long userId) {
        if (userId != null && keyword != null && !keyword.isBlank()) {
            recentSearchService.save(userId, keyword);
        }

        List<ProductDocument> docs = productQueryRepository.search(
                cursorId,
                categoryId,
                limit,
                keyword,
                sortType,
                minPrice,
                maxPrice
        );

        boolean hasNext = docs.size() > limit;
        if (hasNext) {
            docs = docs.subList(0, limit);
        }

        Long nextCursorId = hasNext
                ? docs.get(docs.size() - 1).getProductId()
                : null;

        List<ProductPreviewResponse> responses = docs.stream()
                .map(ProductPreviewResponse::fromDocument)
                .toList();

        return ScrollResponse.of(responses, nextCursorId, hasNext);
    }
}
