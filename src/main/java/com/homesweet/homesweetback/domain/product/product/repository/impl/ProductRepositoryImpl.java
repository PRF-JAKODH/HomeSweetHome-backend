package com.homesweet.homesweetback.domain.product.product.repository.impl;

import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.response.*;
import com.homesweet.homesweetback.domain.product.product.domain.Product;
import com.homesweet.homesweetback.domain.product.product.repository.ProductRepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.ProductJPARepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.repository.mapper.ProductMapper;
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

    private final ProductJPARepository jpaRepository;
    private final ProductMapper mapper;

    @Override
    public Product save(Product product) {
        ProductEntity entity = jpaRepository.save(mapper.toEntity(product));
        return mapper.toDomain(entity);
    }

    @Override
    public boolean existsById(Long productId) {
        return jpaRepository.existsById(productId);
    }

    @Override
    public boolean existsBySellerIdAndName(Long sellerId, String name) {
        return jpaRepository.existsBySellerIdAndName(sellerId, name);
    }

    @Override
    public List<ProductPreviewResponse> findNextProducts(Long cursorId, Long categoryId, int limit, @Nullable String keyword, @NotNull ProductSortType sortType) {
        return jpaRepository.findNextProducts(cursorId, categoryId, limit, keyword, sortType);
    }

    @Override
    public List<SkuStockResponse> findSkuStocksByProductId(Long productId) {
        return jpaRepository.findSkuStocksByProductId(productId);
    }

    @Override
    public ProductDetailResponse findProductDetailById(Long productId) {
        return jpaRepository.findProductDetailById(productId);
    }

    @Override
    public List<ProductManageResponse> findProductsForSeller(Long sellerId) {
        return jpaRepository.findProductsForSeller(sellerId);
    }
}
