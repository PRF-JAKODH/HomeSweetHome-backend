package com.homesweet.homesweetback.domain.product.cart.service.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.product.cart.controller.request.CartRequest;
import com.homesweet.homesweetback.domain.product.cart.domain.Cart;
import com.homesweet.homesweetback.domain.product.cart.repository.CartRepository;
import com.homesweet.homesweetback.domain.product.cart.service.CartService;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.repository.SkuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public Cart addToCart(Long userId, CartRequest request) {
        validateExistsSku(request.skuId());

        return cartRepository.findByUserIdAndSkuId(userId, request.skuId())
                .map(cart -> updateQuantity(cart, request.quantity()))
                .orElseGet(() -> createCart(userId, request.skuId(), request.quantity()));
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
}
