package com.homesweet.homesweetback.domain.product.product.service.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.util.ScrollResponse;
import com.homesweet.homesweetback.domain.product.category.domain.ProductCategory;
import com.homesweet.homesweetback.domain.product.category.domain.exception.ProductCategoryException;
import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.product.controller.request.update.ProductBasicInfoUpdateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.request.create.ProductCreateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.request.update.ProductImageUpdateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.request.update.ProductSkuUpdateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.request.update.ProductStatusUpdateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.response.*;
import com.homesweet.homesweetback.domain.product.product.domain.*;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.repository.ProductRepository;
import com.homesweet.homesweetback.domain.product.product.repository.SkuRepository;
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
@Transactional
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductImageUploader productImageUploader;

    @Override
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
    public ScrollResponse<ProductPreviewResponse> getProductPreview(
            Long cursorId,
            Long categoryId,
            int limit,
            String keyword,
            ProductSortType sortType
    ) {
        List<ProductPreviewResponse> products =
                productRepository.findNextProducts(cursorId, categoryId, limit + 1, keyword, sortType);

        boolean hasNext = products.size() > limit;
        if (hasNext) {
            products = products.subList(0, limit);
        }

        Long nextCursorId = hasNext
                ? products.get(products.size() - 1).id()
                : null;

        return ScrollResponse.of(products, nextCursorId, hasNext);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {

        validateExistsProduct(productId);

        return productRepository.findProductDetailById(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkuStockResponse> getProductStock(Long productId) {

        validateExistsProduct(productId);

        return productRepository.findSkuStocksByProductId(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductManageResponse> getSellerProducts(Long sellerId, String startDate, String endDate) {
        return productRepository.findProductsForSeller(sellerId, startDate, endDate);
    }

    @Override
    public void updateBasicInfo(Long sellerId, Long productId, ProductBasicInfoUpdateRequest request) {
        Product product = productRepository.findByIdAndSellerId(sellerId, productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR));

        if (productRepository.existsBySellerIdAndName(sellerId, request.name())) {
            throw new ProductException(ErrorCode.DUPLICATED_PRODUCT_NAME_ERROR);
        }

        Product update = product.update(request);

        productRepository.update(productId, update);
    }

    @Override
    public void updateSkuStock(Long sellerId, Long productId, ProductSkuUpdateRequest request) {
        Product product = productRepository.findByIdAndSellerId(sellerId, productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR));

        // 각 SKU의 재고 업데이트
        for (ProductSkuUpdateRequest.SkuStockUpdateRequest skuUpdate : request.skus()) {
            Sku sku = skuRepository.findById(skuUpdate.skuId())
                    .orElseThrow(() -> new ProductException(ErrorCode.SKU_NOT_FOUND_ERROR));

            skuRepository.updateSku(sku.getId(), skuUpdate.stockQuantity(), skuUpdate.priceAdjustment());
        }
    }

    @Override
    public void updateProductStatus(Long sellerId, Long productId, ProductStatusUpdateRequest request) {

        Product domain = productRepository.findByIdAndSellerId(sellerId, productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR));

        productRepository.updateStatus(domain.getId(), request.status());
    }

    @Override
    public void updateImages(Long sellerId, Long productId, ProductImageUpdateRequest request) {
        Product product = productRepository.findByIdAndSellerId(sellerId, productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR));

        // 1. 대표 이미지 교체
        if (request.mainImage() != null && !request.mainImage().isEmpty()) {
            productImageUploader.deleteProductImage(product.getImageUrl());

            String newMainImageUrl = productImageUploader.uploadProductMainImage(request.mainImage());

            productRepository.updateMainImage(productId, newMainImageUrl);
        }

        // 2. 상세 이미지 삭제
        if (request.deleteDetailImageUrls() != null && !request.deleteDetailImageUrls().isEmpty()) {
            request.deleteDetailImageUrls().forEach(productImageUploader::deleteProductImage);

            productRepository.deleteDetailImages(productId, request.deleteDetailImageUrls());
        }

        // 3. 상세 이미지 추가
        if (request.detailImages() != null && !request.detailImages().isEmpty()) {
            int currentDetailImageCount = product.getDetailImages().size();
            int newImageCount = request.detailImages().size();

            int deleteCount = request.deleteDetailImageUrls() != null
                    ? request.deleteDetailImageUrls().size()
                    : 0;

            if (currentDetailImageCount - deleteCount + newImageCount > 5) {
                throw new ProductException(ErrorCode.EXCEEDED_IMAGE_LIMIT_ERROR);
            }

            // 새 상세 이미지 업로드
            List<String> newDetailImageUrls = productImageUploader.uploadProductDetailImages(request.detailImages());

            // DB에 새 이미지 추가
            productRepository.addDetailImages(productId, newDetailImageUrls);
        }
    }

    // 상품이 존재하는지 검증하는 로직
    private void validateExistsProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR);
        }
    }
}
