package com.homesweet.homesweetback.domain.product.product.repository;

import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductPreviewResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.SkuStockResponse;
import com.homesweet.homesweetback.domain.product.product.domain.Product;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 제품 레포 인터페이스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
public interface ProductRepository {

    Product save(Product product);

    boolean existsBySellerIdAndName(Long sellerId, String name);

    List<ProductPreviewResponse> findNextProducts(Long cursorId, Long categoryId, int limit, @Nullable String keyword, @NotNull ProductSortType sortType);

    List<SkuStockResponse> findSkuStocksByProductId(Long productId);

    ProductPreviewResponse findProductDetailById(Long productId);
}
