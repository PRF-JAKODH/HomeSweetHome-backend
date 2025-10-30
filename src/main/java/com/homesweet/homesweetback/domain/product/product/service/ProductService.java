package com.homesweet.homesweetback.domain.product.product.service;

import com.homesweet.homesweetback.common.util.ScrollResponse;
import com.homesweet.homesweetback.domain.product.product.controller.request.update.ProductBasicInfoUpdateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.request.create.ProductCreateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.request.update.ProductImageUpdateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.request.update.ProductSkuUpdateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.request.update.ProductStatusUpdateRequest;
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

    ProductDetailResponse getProductDetail(Long productId);

    List<SkuStockResponse> getProductStock(Long productId);

    List<ProductManageResponse> getSellerProducts(Long sellerId, String startDate, String endDate);

    void updateBasicInfo(Long sellerId, Long productId, ProductBasicInfoUpdateRequest request);

    void updateSkuStock(Long sellerId, Long productId, ProductSkuUpdateRequest request);

    void updateProductStatus(Long sellerId, Long productId, ProductStatusUpdateRequest request);

    void updateImages(Long sellerId, Long productId, ProductImageUpdateRequest request);

}
