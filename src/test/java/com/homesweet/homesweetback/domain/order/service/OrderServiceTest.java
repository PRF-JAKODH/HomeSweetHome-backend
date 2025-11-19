package com.homesweet.homesweetback.domain.order.service;

import com.homesweet.homesweetback.common.exception.PaymentMismatchException;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.order.dto.request.CreateOrderRequest;
import com.homesweet.homesweetback.domain.order.dto.response.OrderDetailResponse;
import com.homesweet.homesweetback.domain.order.dto.response.OrderReadyResponse;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.order.repository.PaymentRepository;
import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.SkuJPARepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.ProductJPARepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderItem;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest{

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SkuJPARepository skuJPARepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProductJPARepository productJPARepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("시나리오 1: 단일 상품(옵션가, 배송비 포함) 주문 생성에 성공한다.")
    void createOrder_Success_WithSingleItem() {
        //Given
        Long userId = 1L;
        Long skuId = 100L;
        int quantity = 2;
        long expectedDiscountedPrice = 10000L;
        long expectedShippingPrice = 3000L;
        long expectedTotalAmount = (expectedDiscountedPrice * quantity) + expectedShippingPrice;

        CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest(skuId, quantity);
        CreateOrderRequest dto = new CreateOrderRequest(List.of(itemRequest));

        User fakeUser = User.builder()
                .id(userId)
                .name("테스트유저")
                .build();

        ProductEntity fakeProduct = ProductEntity.builder()
                .id(10L)
                .basePrice(10000)
                .discountRate(new BigDecimal("10.00"))
                .shippingPrice(3000)
                .status(ProductStatus.ON_SALE)
                .build();

        SkuEntity fakeSku = SkuEntity.builder()
                .id(skuId)
                .priceAdjustment(1000)
                .product(fakeProduct)
                .stockQuantity(100L)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(fakeUser));
        given(skuJPARepository.findByIdWithPessimisticLock(skuId)).willReturn(Optional.of(fakeSku));
        given(productJPARepository.findByIdWithPessimisticLock(anyLong())).willReturn(Optional.of(fakeProduct));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        OrderReadyResponse response = orderService.createOrder(dto, userId);

        // THEN
        // 결과 검증
        assertThat(response).isNotNull();
        assertThat(response.totalShippingPrice()).isEqualTo(expectedShippingPrice);
        assertThat(response.totalAmount()).isEqualTo(expectedTotalAmount);

        //행위 검증(가짜 Repository 올바르게 호출했는지 검증)
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(userRepository, times(1)).findById(userId);
        verify(skuJPARepository, times(1)).findByIdWithPessimisticLock(skuId);
        verify(paymentRepository, never()).findByOrder(any(Order.class));
    }

    @Test
    @DisplayName("시나리오 2: 동일 상품의 다른 옵션을 주문해도 배송비가 한 번만 부과된다.")
    void createOrder_Success_WithMultipleItems() {
        // Given
        Long userId = 1L;
        Long productId = 10L;
        Long skuId_S = 100L;
        Long skuId_M = 101L;

        long expectedDiscountedPrice = 9000L;
        long expectedShippingPrice = 3000L;

        long expectedTotalAmount = (expectedDiscountedPrice) + (expectedDiscountedPrice) + expectedShippingPrice;

        CreateOrderRequest.OrderItemRequest itemRequest1 = new CreateOrderRequest.OrderItemRequest(skuId_S, 1);
        CreateOrderRequest.OrderItemRequest itemRequest2 = new CreateOrderRequest.OrderItemRequest(skuId_M, 1);
        CreateOrderRequest dto = new CreateOrderRequest(List.of(itemRequest1, itemRequest2));

        User fakeUser = User.builder().id(userId).build();

        ProductEntity fakeProduct = ProductEntity.builder()
                .id(productId)
                .basePrice(10000)
                .discountRate(new BigDecimal("10.00"))
                .shippingPrice(3000)
                .status(ProductStatus.ON_SALE)
                .build();

        SkuEntity fakeSku_S = SkuEntity.builder()
                .id(skuId_S)
                .priceAdjustment(0)
                .product(fakeProduct)
                .stockQuantity(100L)
                .build();

        SkuEntity fakeSku_M = SkuEntity.builder()
                .id(skuId_M)
                .priceAdjustment(0)
                .product(fakeProduct)
                .stockQuantity(100L)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(fakeUser));
        given(skuJPARepository.findByIdWithPessimisticLock(skuId_S)).willReturn(Optional.of(fakeSku_S));
        given(skuJPARepository.findByIdWithPessimisticLock(skuId_M)).willReturn(Optional.of(fakeSku_M));
        given(productJPARepository.findByIdWithPessimisticLock(anyLong())).willReturn(Optional.of(fakeProduct));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        OrderReadyResponse response = orderService.createOrder(dto, userId);

        // THEN
        assertThat(response).isNotNull();

        assertThat(response.totalShippingPrice()).isEqualTo(expectedShippingPrice);

        assertThat(response.totalAmount()).isEqualTo(expectedTotalAmount);

        verify(skuJPARepository, times(2)).findByIdWithPessimisticLock(anyLong());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("시나리오 3: 서로 다른 상품을 주문하면 배송비가 정상적으로 합산된다.")
    void createOrder_Success_ShippingFeeAddition(){
        // --- GIVEN (주어진 것) ---
        // 1. 상수 정의
        Long userId = 1L;

        // 상품 A (배송비 3000원)
        Long skuId_A = 100L;
        Long productId_A = 10L;
        long price_A = 9000L; // (할인 적용됨)
        long shipping_A = 3000L;

        // 상품 B (배송비 2500원)
        Long skuId_B = 200L;
        Long productId_B = 20L;
        long price_B = 5000L; // (할인 없음)
        long shipping_B = 2500L;

        // [핵심] 기대 배송비 = 3000원 + 2500원 = 5500원
        long expectedShippingPrice = shipping_A + shipping_B;

        // [핵심] 기대 총액 = 9000원 + 5000원 + 5500원
        long expectedTotalAmount = price_A + price_B + expectedShippingPrice; // 19,500L

        // 2. '입력값' DTO 생성 (항목 2개)
        CreateOrderRequest.OrderItemRequest itemRequest1 = new CreateOrderRequest.OrderItemRequest(skuId_A, 1);
        CreateOrderRequest.OrderItemRequest itemRequest2 = new CreateOrderRequest.OrderItemRequest(skuId_B, 1);
        CreateOrderRequest dto = new CreateOrderRequest(List.of(itemRequest1, itemRequest2));

        // 3. '가짜 엔티티' 생성
        User fakeUser = User.builder().id(userId).build();

        // [핵심] 서로 다른 2개의 가짜 상품
        ProductEntity fakeProduct_A = ProductEntity.builder()
                .id(productId_A)
                .basePrice(10000)
                .discountRate(new BigDecimal("10.00")) // 10% 할인
                .shippingPrice((int)shipping_A) // 3000원
                .status(ProductStatus.ON_SALE)
                .build();

        SkuEntity fakeSku_A = SkuEntity.builder()
                .id(skuId_A)
                .priceAdjustment(0)
                .stockQuantity(100L)
                .product(fakeProduct_A) // 👈 상품 A 연결
                .build();

        ProductEntity fakeProduct_B = ProductEntity.builder()
                .id(productId_B)
                .basePrice(5000)
                .discountRate(BigDecimal.ZERO) // 할인 없음
                .shippingPrice((int)shipping_B) // 2500원
                .status(ProductStatus.ON_SALE)
                .build();

        SkuEntity fakeSku_B = SkuEntity.builder()
                .id(skuId_B)
                .priceAdjustment(0)
                .stockQuantity(100L)
                .product(fakeProduct_B) // 👈 상품 B 연결
                .build();

        // 4. 'Mock' Repository의 행동 정의 (Stubbing)
        given(userRepository.findById(userId)).willReturn(Optional.of(fakeUser));

        // [핵심] 2개의 SKU 조회에 각각 다른 상품/SKU를 반환
        given(skuJPARepository.findByIdWithPessimisticLock(skuId_A)).willReturn(Optional.of(fakeSku_A));
        given(skuJPARepository.findByIdWithPessimisticLock(skuId_B)).willReturn(Optional.of(fakeSku_B));
        given(productJPARepository.findByIdWithPessimisticLock(productId_A)).willReturn(Optional.of(fakeProduct_A));
        given(productJPARepository.findByIdWithPessimisticLock(productId_B)).willReturn(Optional.of(fakeProduct_B));
        given(orderRepository.save(any(Order.class)))
                .willAnswer(invocation -> invocation.getArgument(0));


        // --- WHEN (실행) ---
        OrderReadyResponse response = orderService.createOrder(dto, userId);


        // --- THEN (결과) ---
        // 1. '결과값' 검증
        assertThat(response).isNotNull();

        // [핵심 검증] 총 배송비가 5,500원인지?
        assertThat(response.totalShippingPrice()).isEqualTo(expectedShippingPrice);

        // 총 금액이 19,500원이 맞는지?
        assertThat(response.totalAmount()).isEqualTo(expectedTotalAmount);

        // 2. '행위' 검증
        verify(skuJPARepository, times(2)).findByIdWithPessimisticLock(anyLong());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("시나리오 4: 존재하지 않는 SKU ID로 주문하면 EntityNotFoundException이 발생한다.")
    void createOrder_Fail_SkuNotFound() {

        // --- GIVEN (주어진 것) ---
        // 1. 상수 정의
        Long userId = 1L;
        Long nonExistingSkuId = 999L; // [핵심] 존재하지 않는 SKU ID

        // 2. '입력값' DTO 생성
        CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest(nonExistingSkuId, 1);
        CreateOrderRequest dto = new CreateOrderRequest(List.of(itemRequest));

        // 3. '가짜 엔티티' 생성 (User는 필요함)
        User fakeUser = User.builder().id(userId).build();

        // 4. 'Mock' Repository의 행동 정의 (Stubbing)

        // [핵심] userRepository는 정상적으로 User를 반환
        given(userRepository.findById(userId)).willReturn(Optional.of(fakeUser));

        // [핵심] skuJPARepository는 '빈 Optional'을 반환하도록 설정
        given(skuJPARepository.findByIdWithPessimisticLock(nonExistingSkuId)).willReturn(Optional.empty());


        // --- WHEN (실행) & THEN (결과) ---
        // [핵심] "orderService.createOrder()를 실행할 때,
        //        EntityNotFoundException이 발생하는 것을 기대(assert)한다."
        assertThatThrownBy(() -> {
            orderService.createOrder(dto, userId);

            // 1. '예외 타입' 검증1
        }).isInstanceOf(EntityNotFoundException.class)

                // 2. (선택) '예외 메시지' 검증
                .hasMessageContaining("SKU를 찾을 수 없습니다");


        // 3. (가장 중요) '행위' 검증
        // [핵심] 예외가 발생했으므로, 'save'는 절대(never) 호출되면 안 됨.
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("시나리오 5: 존재하지 않는 User ID로 주문하면 EntityNotFoundException이 발생한다.")
    void createOrder_Fail_UserNotFound() {

        // --- GIVEN (주어진 것) ---
        // 1. 상수 정의
        Long nonExistingUserId = 999L; // [핵심] 존재하지 않는 User ID
        Long skuId = 100L; // (어차피 이 SKU는 조회되기 전에 실패할 것임)

        // 2. '입력값' DTO 생성
        CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest(skuId, 1);
        CreateOrderRequest dto = new CreateOrderRequest(List.of(itemRequest));

        // 3. '가짜 엔티티' 생성 (필요 없음)

        // 4. 'Mock' Repository의 행동 정의 (Stubbing)

        // [핵심] userRepository.findById(999L)가 호출되면 '빈 Optional'을 반환
        given(userRepository.findById(nonExistingUserId)).willReturn(Optional.empty());


        // --- WHEN (실행) & THEN (결과) ---
        // "orderService.createOrder()를 실행할 때,
        //  EntityNotFoundException이 발생하는 것을 기대(assert)한다."
        assertThatThrownBy(() -> {
            orderService.createOrder(dto, nonExistingUserId);

            // 1. '예외 타입' 검증
        }).isInstanceOf(EntityNotFoundException.class)

                // 2. (선택) '예외 메시지' 검증
                .hasMessageContaining("사용자를 찾을 수 없습니다");


        // 3. (가장 중요) '행위' 검증
        // [핵심] UserService는 첫 단계에서 실패했으므로,
        //      'sku' 조회나 'order' 저장은 '절대' 호출되면 안 됨.
        verify(skuJPARepository, never()).findByIdWithPessimisticLock(anyLong());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("시나리오 6: 타인의 주문 상세 정보를 조회하면 PaymentMismatchException이 발생한다.")
    void getOrderDetail_Fail_AccessDenied() {

        // --- GIVEN (주어진 것) ---
        // 1. 상수 정의
        Long orderId = 1L;
        Long orderOwnerUserId = 100L; // 👈 주문의 실제 주인
        Long attackerUserId = 999L;   // 👈 [핵심] 주문 조회를 시도하는 사람 (해커)

        // 2. '가짜 엔티티' 생성
        User fakeOrderOwner = User.builder().id(orderOwnerUserId).build();

        Order fakeOrder = Order.builder()
                .id(orderId)
                .user(fakeOrderOwner) // 👈 이 주문은 100번 유저 소유
                .totalAmount(50000L) // (아무 값)
                // (다른 필드는 검증에 필요 없으므로 생략)
                .build();

        // 3. 'Mock'
        // Repository의 행동 정의 (Stubbing)

        // [핵심] orderRepository.findByIdWithDetails(1L)가 호출되면,
        //      '100번 유저'가 주인인 fakeOrder를 반환하도록 설정
        given(orderRepository.findByIdWithDetails(orderId)).willReturn(Optional.of(fakeOrder));

        // --- WHEN (실행) & THEN (결과) ---
        // [핵심] "999번 유저(해커)가 1번 주문(100번 유저 소유) 조회를 시도할 때,
        //        PaymentMismatchException이 발생하는 것을 기대한다."
        assertThatThrownBy(() -> {
            orderService.getOrderDetail(orderId, attackerUserId); // 👈 999L로 호출

            // 1. '예외 타입' 검증
        }).isInstanceOf(PaymentMismatchException.class)

                // 2. '예외 메시지' 검증
                .hasMessageContaining("주문자 정보가 일치하지 않습니다.");

    }

    @Test
    @DisplayName("시나리오 7: getOrderDetail 조회 시, 동일 상품의 여러 옵션 배송비가 한 번만 계산된다.")
    void getOrderDetail_Success_ShippingFeeDeduplication() {

        // --- GIVEN (주어진 것) ---
        // 1. 상수 정의
        Long userId = 100L;
        Long orderId = 1L;
        Long productId = 10L; // [핵심] 동일한 상품 ID

        long expectedShippingPrice = 3000L; // [핵심] 3000원 (6000원이 아님)

        // 2. '가짜 엔티티' 생성
        User fakeUser = User.builder().id(userId).build();

        User fakeSeller = User.builder().id(500L).name("테스트판매자").build();

        // [핵심] 2개의 SKU가 공유할 '하나의' 가짜 상품
        ProductEntity fakeProduct = ProductEntity.builder()
                .id(productId)
                .shippingPrice(3000) // 배송비 3000원
                .seller(fakeSeller)
                .build();

        SkuEntity fakeSku_S = SkuEntity.builder().id(100L).product(fakeProduct).build();
        SkuEntity fakeSku_M = SkuEntity.builder().id(101L).product(fakeProduct).build();

        // [핵심] 2개의 OrderItem 생성
        OrderItem item1_S = OrderItem.builder().id(1L).sku(fakeSku_S).price(10000L).quantity(1L).build();
        OrderItem item2_M = OrderItem.builder().id(2L).sku(fakeSku_M).price(10000L).quantity(1L).build();

        // [핵심] 2개의 OrderItem을 포함하는 '가짜 주문'
        Order fakeOrder = Order.builder()
                .id(orderId)
                .user(fakeUser) // 주문 주인
                .orderItems(List.of(item1_S, item2_M)) // 👈 [중요] S, M 옵션 2개 포함
                .orderedAt(LocalDateTime.now())
                .orderStatus(OrderStatus.PENDING)
                .deliveryStatus(DeliveryStatus.BEFORE_SHIPMENT)
                .build();

        // (OrderItem의 양방향 연관관계 설정 - 실제 코드에서는 addOrderItem이 처리함)
        item1_S.setOrder(fakeOrder);
        item2_M.setOrder(fakeOrder);


        // 3. 'Mock' Repository의 행동 정의 (Stubbing)

        // [핵심] orderRepository가 S, M 옵션이 포함된 fakeOrder를 반환
        given(orderRepository.findByIdWithDetails(orderId)).willReturn(Optional.of(fakeOrder));

        // paymentRepository는 빈 값을 반환 (결제 전)
        given(paymentRepository.findByOrder(fakeOrder)).willReturn(Optional.empty());

        // (OrderDetailResponse.of()는 static DTO 헬퍼이므로 Mocking하지 않고,
        //  실제 반환된 DTO의 값을 검증합니다.
        //  이 테스트를 위해 OrderDetailResponse.java에 'totalShippingPrice' 필드와 getter가 필요합니다.)


        // --- WHEN (실행) ---
        // '진짜' getOrderDetail 메서드를 호출
        OrderDetailResponse response = orderService.getOrderDetail(orderId, userId);


        // --- THEN (결과) ---
        // 1. '결과값(Response)' 검증
        assertThat(response).isNotNull();

        // [가장 중요]
        // OrderService.getOrderDetail 내부의 stream().distinct() 로직이
        // 중복을 제거하여 3000원을 계산했는지 검증합니다.
        // (이 테스트를 위해 OrderDetailResponse에 totalShippingPrice 필드와 getter가 있다고 가정)
        assertThat(response.totalShippingPrice()).isEqualTo((int)expectedShippingPrice);

        // 2. '행위' 검증
        verify(orderRepository, times(1)).findByIdWithDetails(orderId);
    }

    @Test
    @DisplayName("시나리오 8: 판매 중인 상품(ON_SALE)이 아니면 주문 생성에 실패한다.")
    void createOrder_Fail_ProductNotOnSale() {

        // --- GIVEN ---
        Long userId = 1L;
        Long skuId = 100L;

        // DTO 생성
        CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest(skuId, 1);
        CreateOrderRequest dto = new CreateOrderRequest(List.of(itemRequest));

        // 가짜 엔티티 생성
        User fakeUser = User.builder().id(userId).build();

        // [핵심] 상태가 'SUSPENDED' (판매 중지)인 상품
        ProductEntity fakeProduct = ProductEntity.builder()
                .id(10L)
                .status(ProductStatus.SUSPENDED) // 👈 판매 중지 상태
                .build();

        SkuEntity fakeSku = SkuEntity.builder()
                .id(skuId)
                .product(fakeProduct)
                .build();

        // Mock 행동 정의
        given(userRepository.findById(userId)).willReturn(Optional.of(fakeUser));
        // (주의: 비관적 락 메서드를 사용하므로 이것을 Mocking 해야 함)
        given(skuJPARepository.findByIdWithPessimisticLock(skuId)).willReturn(Optional.of(fakeSku));
        given(productJPARepository.findByIdWithPessimisticLock(anyLong())).willReturn(Optional.of(fakeProduct));


        // --- WHEN & THEN ---
        // [검증] "판매 중지가 아니므로 예외가 발생해야 한다"
        assertThatThrownBy(() -> {
            orderService.createOrder(dto, userId);
        })
                // (적절한 예외 타입 사용. ProductException 또는 RuntimeException)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("판매 중인 상품이 아닙니다"); // (메시지는 나중에 구현할 것과 일치시킴)
    }
}
