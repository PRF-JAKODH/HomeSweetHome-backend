package com.homesweet.homesweetback.domain.product.cart.repository.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.product.cart.controller.response.CartResponse;
import com.homesweet.homesweetback.domain.product.cart.domain.Cart;
import com.homesweet.homesweetback.domain.product.cart.repository.CartRepository;
import com.homesweet.homesweetback.domain.product.cart.repository.jpa.CartJPARepository;
import com.homesweet.homesweetback.domain.product.cart.repository.jpa.entity.CartEntity;
import com.homesweet.homesweetback.domain.product.cart.repository.mapper.CartMapper;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 장바구니 레포 구현체
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CartRepositoryImpl implements CartRepository {

    private final CartJPARepository jpaRepository;
    private final CartMapper mapper;


    @Override
    public Optional<Cart> findByUserIdAndSkuId(Long userId, Long skuId) {
        return jpaRepository.findByUserIdAndSkuId(userId, skuId)
                .map(mapper::toDomain);
    }

    @Override
    public Cart save(Cart cart) {
        CartEntity entity = jpaRepository.save(mapper.toEntity(cart));
        return mapper.toDomain(entity);
    }

    @Override
    public Cart updateQuantity(Cart domain) {
        CartEntity entity = jpaRepository.findById(domain.id())
                .orElseThrow(() -> new ProductException(ErrorCode.CART_NOT_FOUND_ERROR));

        log.info("Cart quantity update → before: {}, after: {}", entity.getQuantity(), domain.quantity());
        entity.updateQuantity(domain.quantity());

        return mapper.toDomain(entity);
    }

    @Override
    public List<CartResponse> findNextCartItems(Long memberId, Long cursorId, int size) {

        return jpaRepository.findNextCartItems(memberId, cursorId, size);
    }

    @Override
    public boolean existsByIdAndUserId(Long cartId, Long userId) {

        return jpaRepository.existsByIdAndUserId(cartId, userId);
    }

    @Override
    public void deleteById(Long cartId) {

        jpaRepository.deleteById(cartId);
    }

    @Override
    public void deleteAllByUserIdAndCartIdIn(Long userId, List<Long> cartIds) {

        jpaRepository.deleteAllByUserIdAndIdIn(userId, cartIds);
    }

    @Override
    public int countByUserId(Long userId) {
        return jpaRepository.countByUserId(userId);
    }
}
