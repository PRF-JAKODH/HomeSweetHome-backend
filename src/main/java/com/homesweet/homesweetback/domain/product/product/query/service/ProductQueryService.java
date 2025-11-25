package com.homesweet.homesweetback.domain.product.product.query.service;

import com.homesweet.homesweetback.common.util.ScrollResponse;
import com.homesweet.homesweetback.common.util.SearchScrollResponse;
import com.homesweet.homesweetback.domain.product.product.command.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.query.controller.response.ProductPreviewResponse;

import java.util.List;

/**
 * 상품 검색 서비스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 24.
 */
public interface ProductQueryService {

    List<String> autocomplete(String keyword);

    SearchScrollResponse<ProductPreviewResponse> searchProducts(String nextCursor, Long categoryId, String keyword, ProductSortType sortType, Double minPrice, Double maxPrice, int limit, Long userId, List<String> optionFilters);
}
