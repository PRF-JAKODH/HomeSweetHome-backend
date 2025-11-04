package com.homesweet.homesweetback.domain.product.product.service;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.product.cart.repository.CartRepository;
import com.homesweet.homesweetback.domain.product.category.domain.exception.ProductCategoryException;
import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.repository.ProductRepository;
import com.homesweet.homesweetback.domain.product.product.repository.SkuRepository;
import com.homesweet.homesweetback.domain.product.review.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 상품 관련 검증 로직
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 4.
 */
@Component
@RequiredArgsConstructor
public class ProductValidator {

    private final SkuRepository skuRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductReviewRepository productReviewRepository;

    /**
     * 상품명 중복 검증
     */
    public void validateDuplicatedProductName(Long sellerId, String name) {
        if (productRepository.existsBySellerIdAndName(sellerId, name)) {
            throw new ProductException(ErrorCode.DUPLICATED_PRODUCT_NAME_ERROR);
        }
    }

    /**
     *
     */
    public void validateExistsProductIdAndSellerId(Long productId, Long sellerId) {
        if (!productRepository.existsByIdAndSellerId(productId, sellerId)) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR);
        }
    }

    /**
     * 카테고리 이름 중복 검증
     */
    public void validateDuplicateCategoryName(String name) {
        categoryRepository.findByName(name)
                .ifPresent(c -> {
                    throw new ProductCategoryException(ErrorCode.DUPLICATED_CATEGORY_NAME_ERROR);
                });
    }

    public void validateExistsSku(Long skuId) {
        if (!skuRepository.existsById(skuId)) {
            throw new ProductException(ErrorCode.SKU_NOT_FOUND_ERROR);
        }
    }

    public void validateExistsCart(Long cartId, Long userId) {
        if (!cartRepository.existsByIdAndUserId(cartId, userId)) {
            throw new ProductException(ErrorCode.CART_NOT_FOUND_ERROR);
        }
    }

    /**
     * 상품 존재 검증
     */
    public void validateProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR);
        }
    }

    /**
     * 제품이 등록되어 있는지 검증
     */
    public void validateExistProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR);
        }
    }

    /**
     * 사용자가 제품에 대한 리뷰를 이미 등록한 적 있는지 검증
     */
    public void validateDuplicateReview(Long productId, Long userId) {
        if (productReviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new ProductException(ErrorCode.ALREADY_REVIEW_EXISTS);
        }
    }

}
