package com.homesweet.homesweetback.domain.product.product.command.repository.jpa.querydsl;

import com.homesweet.homesweetback.domain.product.product.command.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.command.controller.request.search.ProductFilterRequest;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.ProductDetailResponse;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.ProductManageResponse;
import com.homesweet.homesweetback.domain.product.product.query.controller.response.ProductPreviewResponse;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.SkuStockResponse;

import java.util.List;
import java.util.Optional;

/**
 * 제품 QueryDSL 레포 인터페이스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
public interface CustomProductRepository {

    List<ProductPreviewResponse> findNextProducts(Long cursorId, Long categoryId, int limit, String keyword, ProductSortType sortType);

    List<ProductPreviewResponse> findProductsByOptionFilter(Long cursorId, ProductFilterRequest request, int limit, ProductSortType sortType);

    List<SkuStockResponse> findSkuStocksByProductId(Long productId);

    Optional<ProductDetailResponse> findProductDetailById(Long productId);

    List<ProductManageResponse> findProductsForSeller(Long sellerId, String startDate, String endDate);
}
