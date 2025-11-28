package com.homesweet.homesweetback.domain.search.product.repository;

import com.homesweet.homesweetback.domain.search.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.search.product.repository.document.ProductDocument;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.List;

/**
 * 상품 검색 레포
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 24.
 */
public interface ProductSearchRepository {

    List<String> autocomplete(String keyword);

    SearchHits<ProductDocument> search(String nextCursor, Long categoryId, int limit, String keyword, ProductSortType sortType, Double minPrice, Double maxPrice, List<String> optionFilters);
}
