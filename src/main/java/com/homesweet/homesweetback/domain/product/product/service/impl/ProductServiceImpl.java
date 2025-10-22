package com.homesweet.homesweetback.domain.product.product.service.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.product.category.domain.ProductCategory;
import com.homesweet.homesweetback.domain.product.category.domain.exception.ProductCategoryException;
import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductCreateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductResponse;
import com.homesweet.homesweetback.domain.product.product.domain.Product;
import com.homesweet.homesweetback.domain.product.product.domain.ProductDetailImage;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.repository.ProductRepository;
import com.homesweet.homesweetback.domain.product.product.service.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    @Override
    @Transactional
    public ProductResponse registerProduct(Long sellerId, ProductCreateRequest request) {

        if (productRepository.existsBySellerIdAndName(sellerId, request.name())) {
            throw new ProductException(ErrorCode.DUPLICATED_PRODUCT_NAME_ERROR);
        }

        ProductCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ProductCategoryException(ErrorCode.CANNOT_FOUND_CATEGORY_ERROR));

        List<ProductDetailImage> detailImages = ProductDetailImage.createDetailImages(request.detailImageUrls());



        Product product = Product.createProduct(
                category.id(),
                sellerId,
                request.name(),
                request.imageUrl(),
                request.brand(),
                request.basePrice(),
                request.discountRate(),
                request.description(),
                request.shippingPrice(),
                detailImages,
                options,
                skus
        );

        Product save = productRepository.save(product);

        return ProductResponse.from(save);
    }
}
