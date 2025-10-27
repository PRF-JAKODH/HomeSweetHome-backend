package com.homesweet.homesweetback.domain.product.product.repository;

import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.response.*;
import com.homesweet.homesweetback.domain.product.product.domain.Product;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * 제품 레포 인터페이스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
public interface ProductRepository {

    Product save(Product product);

    boolean existsById(Long productId);

    boolean existsBySellerIdAndName(Long sellerId, String name);

    List<ProductPreviewResponse> findNextProducts(Long cursorId, Long categoryId, int limit, @Nullable String keyword, @NotNull ProductSortType sortType);

    List<SkuStockResponse> findSkuStocksByProductId(Long productId);

    ProductDetailResponse findProductDetailById(Long productId);

    List<ProductManageResponse> findProductsForSeller(Long sellerId, String startDate, String endDate);
}
