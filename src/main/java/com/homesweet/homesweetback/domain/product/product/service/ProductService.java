package com.homesweet.homesweetback.domain.product.product.service;

import com.homesweet.homesweetback.common.util.ScrollResponse;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductCreateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.response.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 제품 서비스 인터페이스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
public interface ProductService {

    ProductResponse registerProduct(Long sellerId, ProductCreateRequest request, MultipartFile mainImage, List<MultipartFile> detailImages);

    ScrollResponse<ProductPreviewResponse> getProductPreview(Long cursorId, Long categoryId, int size, String keyword, ProductSortType sortType);

    ProductPreviewResponse getProductDetail(Long productId);

    List<SkuStockResponse> getProductStock(Long productId);

    List<ProductManageResponse> getSellerProducts(Long sellerId);
}
