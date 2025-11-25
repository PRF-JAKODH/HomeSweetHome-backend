package com.homesweet.homesweetback.domain.product.product.command.service.impl;

import com.homesweet.homesweetback.common.valid.ProductValidator;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.ProductDetailResponse;
import com.homesweet.homesweetback.domain.product.product.command.repository.ProductRepository;
import com.homesweet.homesweetback.domain.product.product.command.service.ProductSearchService;
import com.homesweet.homesweetback.domain.product.recent.service.RecentViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final RecentViewService recentViewService;

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