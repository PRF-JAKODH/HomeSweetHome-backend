package com.homesweet.homesweetback.domain.product.cart.repository.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.product.cart.controller.response.CartResponse;
import com.homesweet.homesweetback.domain.product.cart.domain.Cart;
import com.homesweet.homesweetback.domain.product.cart.repository.jpa.CartJPARepository;
import com.homesweet.homesweetback.domain.product.cart.repository.jpa.entity.CartEntity;
import com.homesweet.homesweetback.domain.product.cart.repository.mapper.CartMapper;
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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;

/**
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 10.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartRepositoryImpl 단위 테스트")
class CartRepositoryImplTest {

    @InjectMocks
    private CartRepositoryImpl repository;

    @Mock
    private CartJPARepository jpaRepository;
    @Mock
    private CartMapper mapper;

    @Nested
    @DisplayName("장바구니 저장")
    class SaveCart {

        @Test
        @DisplayName("성공적으로 장바구니를 저장하면 Cart 도메인 객체를 반환한다")
        void saveCart_success() {
            // given
            Cart cart = createMockCart(1L, 100L);
            CartEntity entity = createMockCartEntity(1L, cart.quantity());

            given(mapper.toEntity(cart)).willReturn(entity);
            given(jpaRepository.save(entity)).willReturn(entity);
            given(mapper.toDomain(entity)).willReturn(cart);

            // when
            Cart result = repository.save(cart);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.skuId()).isEqualTo(100L);
            assertThat(result.quantity()).isEqualTo(cart.quantity());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("cartId로 장바구니를 조회하면 Optional<Cart>를 반환한다")
        void findById_success() {
            // given
            CartEntity entity = createMockCartEntity(1L, 2);
            Cart cart = createMockCart(1L, 100L);

            given(jpaRepository.findById(1L)).willReturn(Optional.of(entity));
            given(mapper.toDomain(entity)).willReturn(cart);

            // when
            Optional<Cart> result = repository.findById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(1L);
            assertThat(result.get().quantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("cartId가 존재하지 않으면 Optional.empty() 반환")
        void findById_notFound() {
            // given
            given(jpaRepository.findById(999L)).willReturn(Optional.empty());

            // when
            Optional<Cart> result = repository.findById(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserIdAndSkuId")
    class FindByUserIdAndSkuId {

        @Test
        @DisplayName("userId와 skuId로 장바구니를 조회하면 Optional<Cart>를 반환한다")
        void findByUserIdAndSkuId_success() {
            // given
            Long userId = 10L;
            Long skuId = 200L;
            CartEntity entity = createMockCartEntity(1L, 3);
            Cart cart = createMockCart(userId, skuId);

            given(jpaRepository.findByUserIdAndSkuId(userId, skuId)).willReturn(Optional.of(entity));
            given(mapper.toDomain(entity)).willReturn(cart);

            // when
            Optional<Cart> result = repository.findByUserIdAndSkuId(userId, skuId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().userId()).isEqualTo(10L);
            assertThat(result.get().skuId()).isEqualTo(200L);
        }

        @Test
        @DisplayName("해당 userId와 skuId의 장바구니가 존재하지 않으면 Optional.empty() 반환")
        void findByUserIdAndSkuId_notFound() {
            // given
            given(jpaRepository.findByUserIdAndSkuId(1L, 999L)).willReturn(Optional.empty());

            // when
            Optional<Cart> result = repository.findByUserIdAndSkuId(1L, 999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findNextCartItems")
    class FindNextCartItems {

        @Test
        @DisplayName("무한 스크롤 장바구니 목록을 조회하면 CartResponse 리스트를 반환한다")
        void findNextCartItems_success() {
            // given
            List<CartResponse> mockList = List.of(
                    createDefaultCartResponse(1L),
                    createDefaultCartResponse(2L)
            );

            given(jpaRepository.findNextCartItems(10L, null, 5)).willReturn(mockList);

            // when
            List<CartResponse> result = repository.findNextCartItems(10L, null, 5);

            // then
            assertThat(result.get(0).productName()).contains("테스트상품1");
            assertThat(result.get(1).productName()).contains("테스트상품2");
        }

        @Test
        @DisplayName("조회 결과가 비어 있으면 빈 리스트를 반환한다")
        void findNextCartItems_empty() {
            // given
            given(jpaRepository.findNextCartItems(10L, null, 5)).willReturn(List.of());

            // when
            List<CartResponse> result = repository.findNextCartItems(10L, null, 5);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateQuantity")
    class UpdateQuantity {

        @Test
        @DisplayName("장바구니 수량을 성공적으로 업데이트한다")
        void updateQuantity_success() {
            // given
            Cart domain = createMockCart(1L, 100L);
            CartEntity entity = createMockCartEntity(domain.id(), 2);

            given(jpaRepository.findById(domain.id())).willReturn(Optional.of(entity));
            given(mapper.toDomain(entity)).willReturn(domain);

            // when
            Cart result = repository.updateQuantity(domain);

            // then
            assertThat(result).isNotNull();
            assertThat(result.quantity()).isEqualTo(domain.quantity());
            verify(jpaRepository).findById(domain.id());
            verify(mapper).toDomain(entity);
        }

        @Test
        @DisplayName("장바구니 ID가 존재하지 않으면 ProductException 발생")
        void updateQuantity_notFound() {
            // given
            Cart domain = createMockCart(1L, 100L);
            given(jpaRepository.findById(domain.id())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> repository.updateQuantity(domain))
                    .isInstanceOf(ProductException.class)
                    .hasMessage(ErrorCode.CART_NOT_FOUND_ERROR.getMessage());
        }
    }

    @Nested
    @DisplayName("existsByIdAndUserId")
    class ExistsByIdAndUserId {

        @Test
        @DisplayName("장바구니 ID와 사용자 ID로 존재 여부를 확인한다")
        void existsByIdAndUserId_success() {
            // given
            given(jpaRepository.existsByIdAndUserId(1L, 10L)).willReturn(true);

            // when
            boolean exists = repository.existsByIdAndUserId(1L, 10L);

            // then
            assertThat(exists).isTrue();
            verify(jpaRepository).existsByIdAndUserId(1L, 10L);
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("장바구니 ID로 항목을 삭제한다")
        void deleteById_success() {
            // given
            willDoNothing().given(jpaRepository).deleteById(1L);

            // when
            repository.deleteById(1L);

            // then
            verify(jpaRepository).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("countByUserId")
    class CountByUserId {

        @Test
        @DisplayName("사용자의 장바구니 아이템 개수를 반환한다")
        void countByUserId_success() {
            // given
            given(jpaRepository.countByUser_Id(10L)).willReturn(5);

            // when
            int count = repository.countByUserId(10L);

            // then
            assertThat(count).isEqualTo(5);
            verify(jpaRepository).countByUser_Id(10L);
        }
    }

    @Nested
    @DisplayName("deleteAllByUserIdAndCartIdIn")
    class DeleteAllByUserIdAndCartIdIn {

        @Test
        @DisplayName("사용자 ID와 cartIds로 여러 항목을 한 번에 삭제한다")
        void deleteAllByUserIdAndCartIdIn_success() {
            // given
            Long userId = 10L;
            List<Long> cartIds = List.of(1L, 2L, 3L);

            willDoNothing().given(jpaRepository).deleteAllByUserIdAndIdIn(userId, cartIds);

            // when
            repository.deleteAllByUserIdAndCartIdIn(userId, cartIds);

            // then
            verify(jpaRepository).deleteAllByUserIdAndIdIn(userId, cartIds);
        }
    }

    @Nested
    @DisplayName("deleteByUserIdAndSkuIdIn")
    class DeleteByUserIdAndSkuIdIn {

        @Test
        @DisplayName("사용자 ID와 SKU 목록으로 여러 항목을 삭제한다")
        void deleteByUserIdAndSkuIdIn_success() {
            // given
            Long userId = 10L;
            List<Long> skuIds = List.of(100L, 200L);

            willDoNothing().given(jpaRepository).deleteByUserIdAndSkuIdIn(userId, skuIds);

            // when
            repository.deleteByUserIdAndSkuIdIn(userId, skuIds);

            // then
            verify(jpaRepository).deleteByUserIdAndSkuIdIn(userId, skuIds);
        }
    }
}