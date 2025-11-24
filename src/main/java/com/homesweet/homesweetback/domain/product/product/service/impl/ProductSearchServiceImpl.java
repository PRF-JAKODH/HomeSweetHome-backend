package com.homesweet.homesweetback.domain.product.product.service.impl;

import com.homesweet.homesweetback.common.util.ScrollResponse;
import com.homesweet.homesweetback.common.valid.ProductValidator;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductDetailResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductPreviewResponse;
import com.homesweet.homesweetback.domain.product.product.repository.ProductRepository;
import com.homesweet.homesweetback.domain.product.product.service.ProductSearchService;
import com.homesweet.homesweetback.domain.product.product.service.RecentSearchService;
import com.homesweet.homesweetback.domain.product.product.service.RecentViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 상품 검색 서비스 구현체
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 13.
 */
@Service
@RequiredArgsConstructor
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ProductValidator productValidator;
    private final ProductRepository productRepository;
    private final RecentSearchService recentSearchService;
    private final RecentViewService recentViewService;

    @Override
    @Transactional(readOnly = true)
    public ScrollResponse<ProductPreviewResponse> search(Long cursorId, Long categoryId, Long userId, int limit, String keyword, ProductSortType sortType) {

        recentSearchService.save(userId, keyword);

        List<ProductPreviewResponse> products =
                productRepository.findNextProducts(cursorId, categoryId, limit + 1, keyword, sortType);

        boolean hasNext = products.size() > limit;
        if (hasNext) {
            products = products.subList(0, limit);
        }

        Long nextCursorId = hasNext
                ? products.get(products.size() - 1).id()
                : null;

        return ScrollResponse.of(products, nextCursorId, hasNext);
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
}