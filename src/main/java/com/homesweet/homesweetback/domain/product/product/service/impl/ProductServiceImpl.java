package com.homesweet.homesweetback.domain.product.product.service.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.product.category.domain.ProductCategory;
import com.homesweet.homesweetback.domain.product.category.domain.exception.ProductCategoryException;
import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductCreateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductPreviewResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductScrollResponse;
import com.homesweet.homesweetback.domain.product.product.domain.*;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.repository.ProductRepository;
import com.homesweet.homesweetback.domain.product.product.repository.util.ProductImageUploader;
import com.homesweet.homesweetback.domain.product.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 제품 서비스 구현 코드
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductImageUploader productImageUploader;

    @Override
    @Transactional
    public ProductResponse registerProduct(Long sellerId, ProductCreateRequest request, MultipartFile mainImage, List<MultipartFile> detailImages) {

        // 판매자는 중복된 이름의 상품을 등록할 수 없다
        if (productRepository.existsBySellerIdAndName(sellerId, request.name())) {
            throw new ProductException(ErrorCode.DUPLICATED_PRODUCT_NAME_ERROR);
        }

        // 제품 등록 시 -> 카테고리 설정, 대표 이미지 설정, 상세 이미지 설정, 옵션 그룹 생성, 옵션 그룹 별 재고 설정이 필요하다
        ProductCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ProductCategoryException(ErrorCode.CANNOT_FOUND_CATEGORY_ERROR));

        ProductImages productImages = productImageUploader.uploadProductImages(mainImage, detailImages);

        List<ProductDetailImage> detailImage = ProductDetailImage.createDetailImages(productImages.detailImageUrls());

        List<ProductOptionGroup> optionGroups = ProductOptionGroup.createOptionGroups(request.optionGroups());

        List<Sku> skus = Sku.createSkus(request.skus(), optionGroups);

        Product product = Product.createProduct(
                category.id(),
                sellerId,
                request.name(),
                productImages.mainImageUrl(),
                request.brand(),
                request.basePrice(),
                request.discountRate(),
                request.description(),
                request.shippingPrice(),
                detailImage,
                optionGroups,
                skus
        );

        Product save = productRepository.save(product);

        return ProductResponse.from(save);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductScrollResponse getProductPreview(Long cursorId, Long categoryId, int limit, String keyword, ProductSortType sortType) {
        List<ProductPreviewResponse> products = productRepository.findNextProducts(cursorId, categoryId, limit + 1, keyword, sortType);

        boolean hasNext = products.size() > limit;
        if (hasNext) {
            products = products.subList(0, limit);
        }

        Long nextCursorId = hasNext ? products.get(products.size() - 1).id() : null;

        return new ProductScrollResponse(products, nextCursorId, hasNext);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductPreviewResponse getProductDetail(Long productId) {
        return null;
    }
}
