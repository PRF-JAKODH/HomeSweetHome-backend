package com.homesweet.homesweetback.domain.product.product.command.repository.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.product.product.command.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.command.controller.request.search.ProductFilterRequest;
import com.homesweet.homesweetback.domain.product.product.controller.response.*;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.ProductDetailResponse;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.ProductManageResponse;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.ProductPreviewResponse;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.SkuStockResponse;
import com.homesweet.homesweetback.domain.product.product.command.domain.Product;
import com.homesweet.homesweetback.domain.product.product.command.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.product.command.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.command.repository.ProductRepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.ProductJPARepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.ProductDetailImageEntity;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.command.repository.mapper.ProductMapper;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 제품 레포 구현 코드
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJPARepository productRepository;
    private final ProductMapper mapper;

    @Override
    public Product save(Product product) {
        ProductEntity entity = mapper.toEntity(product);

        if (entity == null) {
            throw new IllegalStateException("ProductEntity 매핑 실패했습니다");
        }
        return mapper.toDomain(productRepository.save(entity));
    }

    @Override
    public boolean existsById(Long productId) {
        return productRepository.existsById(productId);
    }

    @Override
    public Optional<Product> findByIdAndSellerId(Long productId, Long sellerId) {
        return productRepository.findByIdAndSellerId(productId, sellerId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsBySellerIdAndName(Long sellerId, String name) {
        return productRepository.existsBySellerIdAndName(sellerId, name);
    }

    @Override
    public List<ProductPreviewResponse> findNextProducts(Long cursorId, Long categoryId, int limit, @Nullable String keyword, @NotNull ProductSortType sortType) {
        return productRepository.findNextProducts(cursorId, categoryId, limit, keyword, sortType);
    }

    @Override
    public List<ProductPreviewResponse> findProductsByOptionFilter(
            Long cursorId,
            ProductFilterRequest request,
            int limit,
            ProductSortType sortType
    ) {
        return productRepository.findProductsByOptionFilter(cursorId, request, limit, sortType);
    }

    @Override
    public List<SkuStockResponse> findSkuStocksByProductId(Long productId) {
        return productRepository.findSkuStocksByProductId(productId);
    }

    @Override
    public ProductDetailResponse findProductDetailById(Long productId) {
        return productRepository.findProductDetailById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR));
    }

    @Override
    public List<ProductManageResponse> findProductsForSeller(Long sellerId, String startDate, String endDate) {
        return productRepository.findProductsForSeller(sellerId, startDate, endDate);
    }

    @Override
    public void updateStatus(Long productId, ProductStatus status) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR));

        entity.updateStatus(status);
    }

    @Override
    public void update(Long productId, Product product) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR));

        entity.updateBasicInfo(product);
    }

    @Override
    public void updateMainImage(Long productId, String newImageUrl) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR));

        entity.updateMainImage(newImageUrl);
    }

    @Override
    public void addDetailImages(Long productId, List<String> imageUrls) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR));

        imageUrls.forEach(url -> {
            ProductDetailImageEntity imageEntity = ProductDetailImageEntity.builder()
                    .imageUrl(url)
                    .build();
            entity.addDetailImage(imageEntity);
        });
    }

    @Override
    public void deleteDetailImages(Long productId, List<String> imageUrls) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR));

        entity.removeDetailImagesByUrls(imageUrls);
    }

    @Override
    public Product findByProductId(Long productId) {
        return productRepository.findById(productId)
                .map(mapper::toDomain)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR));
    }
}