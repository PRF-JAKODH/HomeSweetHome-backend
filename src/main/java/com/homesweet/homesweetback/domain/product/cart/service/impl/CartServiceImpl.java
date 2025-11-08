package com.homesweet.homesweetback.domain.product.cart.service.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.util.ScrollResponse;
import com.homesweet.homesweetback.common.valid.ProductValidator;
import com.homesweet.homesweetback.domain.product.cart.controller.request.CartRequest;
import com.homesweet.homesweetback.domain.product.cart.controller.response.CartResponse;
import com.homesweet.homesweetback.domain.product.cart.domain.Cart;
import com.homesweet.homesweetback.domain.product.cart.repository.CartRepository;
import com.homesweet.homesweetback.domain.product.cart.service.CartService;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.repository.SkuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 장바구니 서비스 구현체
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductValidator productValidator;

    /**
     * 장바구니에 상품 추가
     * - 동일 상품 수량 제한: 최대 10개
     * - 제품 종류 제한: 최대 10종류
     */
    @Transactional
    public Cart addToCart(Long userId, CartRequest request) {
        // 장바구니 수량 검증
        request.validateLimitQuantity();

        // 재고가 존재하는지 확인
        productValidator.validateExistsSku(request.skuId());

        // 이미 장바구니에 존재하면 수량 증가
        Optional<Cart> existingCart = cartRepository.findByUserIdAndSkuId(userId, request.skuId());

        if (existingCart.isPresent()) {
            Cart cart = existingCart.get();
            return updateQuantity(cart, request.quantity());
        } else {
            // 신규 상품이면 장바구니 제품 종류 수 확인 (최대 10종류)
            productValidator.validateCartItemTypeLimit(userId);

            return createCart(userId, request.skuId(), request.quantity());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ScrollResponse<CartResponse> getCartItems(Long memberId, Long cursorId, int size) {
        List<CartResponse> carts = cartRepository.findNextCartItems(memberId, cursorId, size + 1);

        boolean hasNext = carts.size() > size;
        if (hasNext) {
            carts = carts.subList(0, size);
        }

        Long nextCursorId = hasNext ? carts.get(carts.size() - 1).id() : null;

        return ScrollResponse.of(carts, nextCursorId, hasNext);
    }

    @Override
    @Transactional
    public void deleteCartItem(Long userId, Long cartId) {
        productValidator.validateExistsCart(cartId, userId);

        cartRepository.deleteById(cartId);
    }

    @Override
    @Transactional
    public void deleteSelectedCartItems(Long userId, List<Long> cartIds) {

        cartRepository.deleteAllByUserIdAndCartIdIn(userId, cartIds);
    }

    @Override
    @Transactional(readOnly = true)
    public int getCartItemCount(Long userId) {
        return cartRepository.countByUserId(userId);
    }

    @Override
    @Transactional
    public void updateCartItemQuantity(Long userId, Long cartId, int quantity) {

        productValidator.validateExistsCart(cartId, userId);

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ProductException(ErrorCode.CART_NOT_FOUND_ERROR));

        Cart updated = cart.updateQuantity(quantity);

        cartRepository.updateQuantity(updated);
    }

    private Cart updateQuantity(Cart cart, int additionalQuantity) {
        Cart domain = cart.updateQuantity(cart.quantity() + additionalQuantity);
        return cartRepository.updateQuantity(domain);
    }

    private Cart createCart(Long userId, Long skuId, Integer quantity) {
        Cart cart = Cart.create(userId, skuId, quantity);

        return cartRepository.save(cart);
    }
}
