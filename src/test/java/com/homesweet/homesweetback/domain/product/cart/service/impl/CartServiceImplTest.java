package com.homesweet.homesweetback.domain.product.cart.service.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.util.scroll.ScrollResponse;
import com.homesweet.homesweetback.common.valid.ProductValidator;
import com.homesweet.homesweetback.domain.product.cart.controller.request.CartRequest;
import com.homesweet.homesweetback.domain.product.cart.controller.response.CartResponse;
import com.homesweet.homesweetback.domain.product.cart.domain.Cart;
import com.homesweet.homesweetback.domain.product.cart.repository.CartRepository;
import com.homesweet.homesweetback.domain.product.product.command.domain.exception.ProductException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.homesweet.homesweetback.domain.product.data.CartMockData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.*;

/**
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 8.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("장바구니 서비스 단위 테스트")
class CartServiceImplTest {

    @InjectMocks
    private CartServiceImpl service;

    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductValidator productValidator;

    @Nested
    @DisplayName("장바구니 생성")
    class CreateCart {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("이미 장바구니에 존재하는 상품은 수량이 증가한다")
            void addToCart_existingCart_updatesQuantity() {
                // given
                Long userId = 1L;
                Long skuId = 10L;
                CartRequest request = new CartRequest(skuId, 3);

                Cart existingCart = createMockCart(userId, skuId);

                Cart updatedCart = existingCart.updateQuantity(5);

                willDoNothing().given(productValidator).validateExistsSku(skuId);
                given(cartRepository.findByUserIdAndSkuId(userId, skuId)).willReturn(Optional.of(existingCart));
                given(cartRepository.updateQuantity(any(Cart.class))).willReturn(updatedCart);

                // when
                Cart result = service.addToCart(userId, request);

                // then
                assertThat(result.quantity()).isEqualTo(5);
            }

            @Test
            @DisplayName("새로운 상품은 장바구니에 추가된다")
            void addToCart_newCart_createsNewCart() {
                // given
                Long userId = 1L;
                Long skuId = 10L;
                CartRequest request = new CartRequest(skuId, 2);

                Cart newCart = createMockCart(userId, skuId);

                willDoNothing().given(productValidator).validateExistsSku(skuId);
                willDoNothing().given(productValidator).validateCartItemTypeLimit(userId);
                given(cartRepository.findByUserIdAndSkuId(userId, skuId)).willReturn(Optional.empty());
                given(cartRepository.save(any(Cart.class))).willReturn(newCart);

                // when
                Cart result = service.addToCart(userId, request);

                // then
                assertThat(result.userId()).isEqualTo(userId);
                assertThat(result.skuId()).isEqualTo(skuId);
                assertThat(result.quantity()).isEqualTo(2);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Fail {

            @Test
            @DisplayName("SKU가 존재하지 않으면 ProductException 발생")
            void addToCart_skuNotFound_throwsException() {
                // given
                Long userId = 1L;
                Long skuId = 999L;
                CartRequest request = new CartRequest(skuId, 1);

                willThrow(new ProductException(ErrorCode.SKU_NOT_FOUND_ERROR))
                        .given(productValidator).validateExistsSku(skuId);

                // when & then
                assertThatThrownBy(() -> service.addToCart(userId, request))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.SKU_NOT_FOUND_ERROR.getMessage());
            }

            @Test
            @DisplayName("요청 수량이 10개 초과이면 ProductException 발생")
            void addToCart_quantityExceedsLimit_throwsException() {
                // given
                Long userId = 1L;
                Long skuId = 10L;
                CartRequest request = new CartRequest(skuId, 11); // 10개 초과

                // when & then
                assertThatThrownBy(() -> service.addToCart(userId, request))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.CART_LIMIT_EXCEEDED_ERROR.getMessage());
            }
        }

        @Test
        @DisplayName("장바구니 내 상품 종류가 10종류를 초과하면 ProductException 발생")
        void addToCart_cartItemTypeLimitExceeded_throwsException() {
            // given
            Long userId = 1L;
            Long skuId = 11L;
            CartRequest request = new CartRequest(skuId, 1);

            willDoNothing().given(productValidator).validateExistsSku(skuId);
            willThrow(new ProductException(ErrorCode.CART_ITEM_TYPE_LIMIT_EXCEEDED_ERROR))
                    .given(productValidator).validateCartItemTypeLimit(userId);

            given(cartRepository.findByUserIdAndSkuId(userId, skuId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.addToCart(userId, request))
                    .isInstanceOf(ProductException.class)
                    .hasMessage(ErrorCode.CART_ITEM_TYPE_LIMIT_EXCEEDED_ERROR.getMessage());
        }
    }

    @Nested
    @DisplayName("장바구니 조회")
    class FindCarts {
        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("장바구니를 페이징 단위로 조회할 수 있다 (hasNext = true)")
            void getCartItems_hasNextTrue() {
                // given
                Long memberId = 1L;
                Long cursorId = null;
                int size = 2;

                CartResponse item1 = createDefaultCartResponse(1L);
                CartResponse item2 = createDefaultCartResponse(2L);
                CartResponse item3 = createDefaultCartResponse(3L);

                List<CartResponse> cartResponses = List.of(item1, item2, item3);

                given(cartRepository.findNextCartItems(memberId, cursorId, size + 1))
                        .willReturn(cartResponses);

                // when
                ScrollResponse<CartResponse> result = service.getCartItems(memberId, cursorId, size);

                // then
                assertThat(result.contents()).hasSize(2);
                assertThat(result.hasNext()).isTrue();
                assertThat(result.nextCursorId()).isEqualTo(2L);
            }

            @Test
            @DisplayName("마지막 페이지에서는 hasNext = false가 된다")
            void getCartItems_lastPage_hasNextFalse() {
                // given
                Long memberId = 1L;
                Long cursorId = 2L;
                int size = 2;

                CartResponse item1 = createDefaultCartResponse(1L);
                CartResponse item2 = createDefaultCartResponse(2L);

                List<CartResponse> cartResponses = List.of(item1, item2);

                given(cartRepository.findNextCartItems(memberId, cursorId, size + 1))
                        .willReturn(cartResponses);

                // when
                ScrollResponse<CartResponse> result = service.getCartItems(memberId, cursorId, size);

                // then
                assertThat(result.contents()).hasSize(2);
                assertThat(result.hasNext()).isFalse();
                assertThat(result.nextCursorId()).isNull();
            }

            @Test
            @DisplayName("사용자의 장바구니에 담긴 상품 개수를 조회할 수 있다")
            void getCartItemCount_success() {
                // given
                Long userId = 1L;
                int expectedCount = 5;

                given(cartRepository.countByUserId(userId)).willReturn(expectedCount);

                // when
                int result = service.getCartItemCount(userId);

                // then
                assertThat(result).isEqualTo(expectedCount);
            }

            @Test
            @DisplayName("장바구니에 담긴 상품이 없으면 0을 반환한다")
            void getCartItemCount_emptyCart_returnsZero() {
                // given
                Long userId = 1L;
                given(cartRepository.countByUserId(userId)).willReturn(0);

                // when
                int result = service.getCartItemCount(userId);

                // then
                assertThat(result).isZero();
            }
        }
    }

    @Nested
    @DisplayName("장바구니 상품 제거")
    class DeleteCartItem {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("단일 장바구니 품목을 정상적으로 삭제할 수 있다")
            void deleteCartItem_success() {
                // given
                Long userId = 1L;
                Long cartId = 100L;

                // 존재 여부 검증 성공
                willDoNothing().given(productValidator).validateExistsCart(cartId, userId);
                willDoNothing().given(cartRepository).deleteById(cartId);

                // when
                service.deleteCartItem(userId, cartId);

                // then
                verify(cartRepository).deleteById(cartId);
            }

            @Test
            @DisplayName("여러 장바구니 품목을 한 번에 삭제할 수 있다")
            void deleteSelectedCartItems_success() {
                // given
                Long userId = 1L;
                List<Long> cartIds = List.of(101L, 102L, 103L);

                willDoNothing().given(cartRepository).deleteAllByUserIdAndCartIdIn(userId, cartIds);

                // when
                service.deleteSelectedCartItems(userId, cartIds);

                // then
                verify(cartRepository).deleteAllByUserIdAndCartIdIn(userId, cartIds);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Fail {

            @Test
            @DisplayName("존재하지 않는 장바구니 품목을 삭제하려 하면 ProductException 발생")
            void deleteCartItem_notFound_throwsException() {
                // given
                Long userId = 1L;
                Long invalidCartId = 999L;

                willThrow(new ProductException(ErrorCode.CART_NOT_FOUND_ERROR))
                        .given(productValidator).validateExistsCart(invalidCartId, userId);

                // when & then
                assertThatThrownBy(() -> service.deleteCartItem(userId, invalidCartId))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.CART_NOT_FOUND_ERROR.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("장바구니 수량 변경")
    class UpdateCartQuantity {
        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("장바구니 상품 수량을 정상적으로 변경할 수 있다")
            void updateCartItemQuantity_success() {
                // given
                Long userId = 1L;
                Long cartId = 100L;
                int newQuantity = 5;

                Cart existingCart = createMockCart(userId, cartId);

                Cart updatedCart = existingCart.updateQuantity(newQuantity);

                willDoNothing().given(productValidator).validateExistsCart(cartId, userId);
                given(cartRepository.findById(cartId)).willReturn(Optional.of(existingCart));
                given(cartRepository.updateQuantity(any(Cart.class))).willReturn(updatedCart);

                // when
                service.updateCartItemQuantity(userId, cartId, newQuantity);

                // then
                verify(productValidator).validateExistsCart(cartId, userId);
                verify(cartRepository).findById(cartId);
                verify(cartRepository).updateQuantity(any(Cart.class));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Fail {

            @Test
            @DisplayName("장바구니가 존재하지 않으면 ProductException 발생")
            void updateCartItemQuantity_notFound_throwsException() {
                // given
                Long userId = 1L;
                Long invalidCartId = 999L;
                int newQuantity = 5;

                willDoNothing().given(productValidator).validateExistsCart(invalidCartId, userId);
                given(cartRepository.findById(invalidCartId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> service.updateCartItemQuantity(userId, invalidCartId, newQuantity))
                        .isInstanceOf(ProductException.class)
                        .hasMessage(ErrorCode.CART_NOT_FOUND_ERROR.getMessage());
            }
        }
    }
}