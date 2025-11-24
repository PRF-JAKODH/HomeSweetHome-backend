package com.homesweet.homesweetback.domain.product.product.query.repository;

import java.util.List;

/**
 * 상품 검색 레포
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 24.
 */
public interface ProductQueryRepository {

    List<String> autocomplete(String keyword);
}
