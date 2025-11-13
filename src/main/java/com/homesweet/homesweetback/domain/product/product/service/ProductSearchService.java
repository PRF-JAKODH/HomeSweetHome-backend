package com.homesweet.homesweetback.domain.product.product.service;

import com.homesweet.homesweetback.common.util.ScrollResponse;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductDetailResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductPreviewResponse;

/**
 * 상품 조회 서비스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 13.
 */
public interface ProductSearchService {

    ScrollResponse<ProductPreviewResponse> search(Long cursorId, Long categoryId, Long userId, int limit, String keyword, ProductSortType sortType);

    ProductDetailResponse getProductDetail(Long userId, Long productId);
}
