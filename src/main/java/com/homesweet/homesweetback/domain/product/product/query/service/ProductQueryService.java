package com.homesweet.homesweetback.domain.product.product.query.service;

import java.util.List;

/**
 * 상품 검색 서비스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 24.
 */
public interface ProductQueryService {

    List<String> autocomplete(String keyword);
}
