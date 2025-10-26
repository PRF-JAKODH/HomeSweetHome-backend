package com.homesweet.homesweetback.domain.product.cart.service.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.util.ScrollResponse;
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
    private final SkuRepository skuRepository;

    /**
     * 장바구니에 상품 추가
     * - 동일 상품 수량 제한: 최대 10개
     * - 제품 종류 제한: 최대 10종류
     */
    @Transactional
    public Cart addToCart(Long userId, CartRequest request) {
        validateExistsSku(request.skuId());

        // 이미 장바구니에 존재하면 수량 증가
        Optional<Cart> existingCart = cartRepository.findByUserIdAndSkuId(userId, request.skuId());

        if (existingCart.isPresent()) {
            // 기존 상품이면 수량만 증가
            Cart cart = existingCart.get();
            int newQuantity = cart.quantity() + request.quantity();

            // 동일 상품 수량 제한 체크 (최대 10개)
            if (newQuantity > 10) {
                throw new ProductException(ErrorCode.CART_LIMIT_EXCEEDED_ERROR);
            }

            cart.updateQuantity(newQuantity);
            return cartRepository.save(cart);
        } else {
            // 신규 상품이면 장바구니 제품 종류 수 확인 (최대 10종류)
            int cartItemCount = cartRepository.countByUserId(userId);
            if (cartItemCount >= 10) {
                throw new ProductException(ErrorCode.CART_ITEM_TYPE_LIMIT_EXCEEDED_ERROR);
            }

            // 수량 제한 체크
            if (request.quantity() > 10) {
                throw new ProductException(ErrorCode.CART_LIMIT_EXCEEDED_ERROR);
            }

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
        validateExistsCart(cartId, userId);

        cartRepository.deleteById(cartId);
    }

    @Override
    @Transactional
    public void deleteSelectedCartItems(Long userId, List<Long> cartIds) {

        cartRepository.deleteAllByUserIdAndCartIdIn(userId, cartIds);
    }

    private Cart updateQuantity(Cart cart, int additionalQuantity) {
        Cart domain = cart.updateQuantity(cart.quantity() + additionalQuantity);
        return cartRepository.updateQuantity(domain);
    }

    private Cart createCart(Long userId, Long skuId, Integer quantity) {
        Cart cart = Cart.create(userId, skuId, quantity);

        return cartRepository.save(cart);
    }


    private void validateExistsSku(Long skuId) {
        if (!skuRepository.existsById(skuId)) {
            throw new ProductException(ErrorCode.SKU_NOT_FOUND_ERROR);
        }
    }

    private void validateExistsCart(Long cartId, Long userId) {
        if (!cartRepository.existsByIdAndUserId(cartId, userId)) {
            throw new ProductException(ErrorCode.CART_NOT_FOUND_ERROR);
        }
    }
}
