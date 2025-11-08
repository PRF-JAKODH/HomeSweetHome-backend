package com.homesweet.homesweetback.common.valid;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.product.cart.repository.CartRepository;
import com.homesweet.homesweetback.domain.product.category.domain.exception.ProductCategoryException;
import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.product.domain.Product;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.repository.ProductRepository;
import com.homesweet.homesweetback.domain.product.product.repository.SkuRepository;
import com.homesweet.homesweetback.domain.product.review.domain.ProductReview;
import com.homesweet.homesweetback.domain.product.review.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 제품 검증 클래스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 6.
 */
@Service
@RequiredArgsConstructor
public class ProductValidator {

    private static final int MAX_DETAIL_IMAGE_COUNT = 5;
    private final CartRepository cartRepository;
    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductReviewRepository productReviewRepository;

    /**
     * 동일한 카테고리 명이 있는지 검증
     */
    public void validateDuplicateCategoryName(String name) {
        categoryRepository.findByName(name)
                .ifPresent(c -> {
                    throw new ProductCategoryException(ErrorCode.DUPLICATED_CATEGORY_NAME_ERROR);
                });
    }

    /**
     * 장바구니가 존재하는지 검증
     */
    public void validateExistsCart(Long cartId, Long userId) {
        if (!cartRepository.existsByIdAndUserId(cartId, userId)) {
            throw new ProductException(ErrorCode.CART_NOT_FOUND_ERROR);
        }
    }

    /**
     * 판매자가 이미 동일한 상품명을 등록했는지 검증
     */
    public void validateDuplicateProductName(Long sellerId, String name) {
        if (productRepository.existsBySellerIdAndName(sellerId, name)) {
            throw new ProductException(ErrorCode.DUPLICATED_PRODUCT_NAME_ERROR);
        }
    }

    /**
     * 판매자가 해당 상품을 등록했는지 검증
     */
    public void validateExistsSellerProduct(Long productId, Long sellerId) {
        productRepository.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR));
    }

    /**
     * 상품이 존재하는지 검증
     */
    public void validateExistsProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND_ERROR);
        }
    }

    /**
     * 상세 이미지 개수 검증 (5장 초과 불가)
     */
    public void validateDetailImageLimit(Product product, List<String> deleteUrls, List<?> newImages) {
        int current = product.getDetailImages().size();
        int deleted = (deleteUrls != null) ? deleteUrls.size() : 0;
        int added = (newImages != null) ? newImages.size() : 0;

        if (current - deleted + added > MAX_DETAIL_IMAGE_COUNT) {
            throw new ProductException(ErrorCode.EXCEEDED_IMAGE_LIMIT_ERROR);
        }
    }

    /**
     * 재고가 존재하는지 검증
     */
    public void validateExistsSku(Long skuId) {
        if (!skuRepository.existsById(skuId)) {
            throw new ProductException(ErrorCode.SKU_NOT_FOUND_ERROR);
        }
    }

    /**
     * 신규 상품이면 장바구니 제품 종류 수 확인 (최대 10종류)
     */
    public void validateCartItemTypeLimit(Long userId) {
        int cartItemCount = cartRepository.countByUserId(userId);
        if (cartItemCount >= 10) {
            throw new ProductException(ErrorCode.CART_ITEM_TYPE_LIMIT_EXCEEDED_ERROR);
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

    /**
     * 이미 리뷰를 작성한 적이 있는지 검증
     */
    public void validateDuplicateWriter(ProductReview productReview, Long userId) {
        if (!productReview.userId().equals(userId)) {
            throw new ProductException(ErrorCode.PRODUCT_REVIEW_FORBIDDEN);
        }
    }
}
