package com.homesweet.homesweetback.domain.product.product.repository.jpa.querydsl;

import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductPreviewResponse;

import java.util.List;

/**
 * 제품 QueryDSL 레포 인터페이스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
public interface CustomProductRepository {

    List<ProductPreviewResponse> findNextProducts(Long cursorId, int size, String keyword, ProductSortType sortType);
}
