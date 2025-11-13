package com.homesweet.homesweetback.domain.product.product.repository;

import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.request.search.ProductFilterRequest;
import com.homesweet.homesweetback.domain.product.product.controller.response.*;
import com.homesweet.homesweetback.domain.product.product.domain.Product;
import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;
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

    Optional<Product> findByIdAndSellerId(Long productId, Long sellerId);

    boolean existsBySellerIdAndName(Long sellerId, String name);

    List<ProductPreviewResponse> findNextProducts(Long cursorId, Long categoryId, int limit, @Nullable String keyword, @NotNull ProductSortType sortType);

    List<ProductPreviewResponse> findProductsByOptionFilter(Long cursorId, ProductFilterRequest request, int limit, ProductSortType sortType);

    List<SkuStockResponse> findSkuStocksByProductId(Long productId);

    ProductDetailResponse findProductDetailById(Long productId);

    List<ProductManageResponse> findProductsForSeller(Long sellerId, String startDate, String endDate);

    void updateStatus(Long productId, ProductStatus status);

    void update(Long productId, Product product);

    void updateMainImage(Long productId, String newImageUrl);

    void addDetailImages(Long productId, List<String> imageUrls);

    void deleteDetailImages(Long productId, List<String> imageUrls);

    // 알림용
    Product findByProductId(Long productId);
}
