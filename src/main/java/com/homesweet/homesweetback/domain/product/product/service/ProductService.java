package com.homesweet.homesweetback.domain.product.product.service;

import com.homesweet.homesweetback.domain.product.product.controller.request.ProductCreateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductPreviewResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductScrollResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.SkuStockResponse;
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

    ProductScrollResponse getProductPreview(Long cursorId, Long categoryId, int size, String keyword, ProductSortType sortType);

    ProductPreviewResponse getProductDetail(Long productId);

    List<SkuStockResponse> getProductStock(Long productId);
}
