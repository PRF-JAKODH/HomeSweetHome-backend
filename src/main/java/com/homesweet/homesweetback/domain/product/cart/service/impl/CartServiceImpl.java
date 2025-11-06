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

    // 장바구니 수량 변경용 - 안채호
    @Override
    @Transactional
    public void updateCartItemQuantity(Long userId, Long cartId, int quantity) {

        // 1. 수량 유효성 검사 (프론트에서 1로 막지만, 서버에서도 방어)
        if (quantity <= 0) {
            // (참고) 프론트엔드 cart/page.tsx는 수량이 0이 되면
            // 이 메서드 대신 deleteCartItem을 호출하도록 되어있습니다.
            // 하지만 비정상적인 0이하 값 요청은 막습니다.
            throw new ProductException(ErrorCode.INVALID_INPUT_VALUE); // (적절한 에러 코드로 변경 필요)
        }

        // 2. 수량 10개 제한 체크 (addToCart와 동일한 정책 적용)
        if (quantity > 10) {
            throw new ProductException(ErrorCode.CART_LIMIT_EXCEEDED_ERROR);
        }

        // 3. 카트 항목이 사용자의 소유인지 검증 (기존 헬퍼 메서드 재사용)
        productValidator.validateExistsCart(cartId, userId);

        // 4. 카트 정보 가져오기
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ProductException(ErrorCode.CART_NOT_FOUND_ERROR));

        // 5. Cart 도메인 객체의 수량 업데이트 메서드 호출
        // (참고: CartEntity.updateQuantity가 quantity를 '설정'한다고 가정)
        Cart updatedCartDomain = cart.updateQuantity(quantity);

        // 6. Repository를 통해 DB에 수량 업데이트
        // (참고: addToCart의 private updateQuantity와 동일한 방식)
        cartRepository.updateQuantity(updatedCartDomain);
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
