package com.homesweet.homesweetback.domain.product.product.query.repository;

import com.homesweet.homesweetback.domain.product.product.command.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.query.repository.document.ProductDocument;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.List;

/**
 * 상품 검색 레포
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 24.
 */
public interface ProductQueryRepository {

    List<String> autocomplete(String keyword);

    SearchHits<ProductDocument> search(String nextCursor, Long categoryId, int limit, String keyword, ProductSortType sortType, Double minPrice, Double maxPrice, List<String> optionFilters);
}
