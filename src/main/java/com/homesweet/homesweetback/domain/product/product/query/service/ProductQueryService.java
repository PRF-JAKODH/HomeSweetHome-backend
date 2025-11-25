package com.homesweet.homesweetback.domain.product.product.query.service;

import com.homesweet.homesweetback.common.util.ScrollResponse;
import com.homesweet.homesweetback.domain.product.product.command.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.ProductPreviewResponse;

import java.util.List;

/**
 * 상품 검색 서비스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 24.
 */
public interface ProductQueryService {

    List<String> autocomplete(String keyword);

    ScrollResponse<ProductPreviewResponse> searchProducts(Long cursorId, Long categoryId, String keyword, ProductSortType sortType, Double minPrice, Double maxPrice, int limit, Long userId);
}
