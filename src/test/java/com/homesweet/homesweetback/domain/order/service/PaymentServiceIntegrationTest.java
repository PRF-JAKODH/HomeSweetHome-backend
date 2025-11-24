package com.homesweet.homesweetback.domain.order.service;

// (필요한 import문들)
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.order.adapter.TossPaymentsAdapter;
import com.homesweet.homesweetback.domain.order.dto.request.OrderCancelRequest;
import com.homesweet.homesweetback.domain.order.dto.request.PaymentConfirmRequest;
import com.homesweet.homesweetback.domain.order.dto.response.PaymentConfirmResponse;
import com.homesweet.homesweetback.domain.order.entity.*;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.order.repository.PaymentRepository;
import com.homesweet.homesweetback.domain.product.cart.controller.response.CartResponse;
import com.homesweet.homesweetback.domain.product.cart.domain.Cart;
import com.homesweet.homesweetback.domain.product.cart.repository.CartRepository;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.ProductCategoryJPARepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.ProductJPARepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.SkuJPARepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.command.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.SkuEntity;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean; // 👈 2. [추가] @MockBean import
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given; // 👈 3. [추가] given import

@SpringBootTest
@Transactional // 테스트 후 DB 롤백
@ActiveProfiles("test")
class PaymentServiceIntegrationTest {

    // --- 4. [수정] "진짜" Service와 Repository 주입 ---
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SkuJPARepository skuJPARepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private ProductCategoryJPARepository productCategoryJPARepository;
    @Autowired
    private ProductJPARepository productJPARepository;

    // --- 5. [핵심] "가짜" Adapter Bean 주입 ---
    @MockBean // 👈 @Mock이 아닌 @MockBean
    private TossPaymentsAdapter tossPaymentsAdapter;

    // (테스트 간 데이터를 공유할 필드)
    private User savedUser;
    private SkuEntity savedSku;
    private Order savedPendingOrder; // 👈 결제할 'PENDING' 주문
    private final long INITIAL_STOCK = 10L;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("testuser@example.com")
                .name("테스트유저")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        savedUser = userRepository.save(user);

        ProductCategoryEntity category = ProductCategoryEntity.builder()
                .name("테스트 카테고리")
                .depth(0)
                .build();
        ProductCategoryEntity savedCategoryEntity = productCategoryJPARepository.save(category);

        ProductEntity product = ProductEntity.builder()
                .name("테스트 상품")
                .basePrice(10000)
                .discountRate(new BigDecimal("10.00")) // 10%
                .shippingPrice(3000)
                .brand("테스트브랜드")
                .category(savedCategoryEntity)
                .seller(savedUser)
                .imageUrl("http://example.com/image.jpg")
                .status(ProductStatus.ON_SALE)
                .build();
        ProductEntity savedProduct = productJPARepository.save(product);

        SkuEntity sku = SkuEntity.builder()
                .product(savedProduct)
                .priceAdjustment(1000) // 옵션가 +1000
                .stockQuantity(INITIAL_STOCK) // 👈 초기 재고 10개
                .build();
        savedSku = skuJPARepository.save(sku);

        // [핵심] PaymentService 테스트를 위해 "재고가 차감된 PENDING 주문"을 미리 저장
        // (OrderService의 createOrder 로직을 여기서 간단히 흉내 냄)
        Order order = Order.builder()
                .user(savedUser)
                .orderNumber("ORD-PAYMENT-INTEGRATION-TEST")
                .totalAmount(13000L) // (상품 10000 + 배송비 3000)
                .orderStatus(OrderStatus.PENDING) // 👈 PENDING 상태
                .deliveryStatus(DeliveryStatus.BEFORE_SHIPMENT)
                .orderedAt(LocalDateTime.now())
                .build();
        // (OrderService가 재고를 차감했다고 가정. 여기서는 재고 차감 로직은 테스트하지 않음)

        OrderItem orderItem = OrderItem.builder()
                .sku(savedSku) // 👈 @BeforeEach에서 저장한 Sku
                .quantity(1L)
                .price(10000L) // (OrderService가 계산한 가격)
                .build();
        order.addOrderItem(orderItem); // 👈 [핵심] Order에 OrderItem 추가

        savedPendingOrder = orderRepository.save(order);

        // [핵심] PaymentService가 삭제할 '장바구니' 데이터를 미리 저장
        Cart cartItem = Cart.create(savedUser.getId(), savedSku.getId(), 1); // 👈 Sku 1개
        cartRepository.save(cartItem);
    }
    @Test
    @DisplayName("confirmPayment 통합 테스트: API 성공 시 DB 작업(Payment 저장, Cart 삭제, Order 상태 변경)이 성공한다.")
    void confirmPayment_IntegrationTest_Success() {

        // --- GIVEN (준비) ---
        // 1. '입력값' DTO 생성 (@BeforeEach의 savedPendingOrder 정보와 일치)
        PaymentConfirmRequest dto = new PaymentConfirmRequest(
                "pk_test_payment_123",
                savedPendingOrder.getOrderNumber(), // 👈 @BeforeEach에서 저장한 주문번호
                savedPendingOrder.getTotalAmount()  // 👈 @BeforeEach에서 저장한 총액
        );
        Long userId = savedUser.getId();

        // 2. '가짜' Toss 응답 데이터 생성
        Map<String, Object> tossResponse = Map.of(
                "paymentKey", "pk_test_payment_123",
                "method", "카드",
                "status", "DONE",
                "paidAt", "2025-11-17T10:00:00"
        );

        // 3. [핵심] @MockBean으로 등록된 '가짜' 어댑터의 행동 정의
        // "tossPaymentsAdapter.confirmPaymentToToss(dto)가 호출되면,
        //  (실제 API 호출 대신) '가짜 성공 응답'을 반환해라."
        given(tossPaymentsAdapter.confirmPaymentToToss(dto)).willReturn(tossResponse);


        // --- WHEN (실행) ---
        // [핵심] "진짜" PaymentService의 "진짜" confirmPayment 메서드를 호출
        PaymentConfirmResponse response = paymentService.confirmPayment(dto, userId);


        // --- THEN (검증) ---
        // 1. [DTO 검증] 반환된 DTO의 상태가 'COMPLETED'인지?
        assertThat(response.orderId()).isEqualTo(savedPendingOrder.getId());
        assertThat(response.status()).isEqualTo(OrderStatus.COMPLETED.name());

        // 2. [DB 검증 1: Order] '진짜' H2 DB를 조회
        Order updatedOrder = orderRepository.findById(savedPendingOrder.getId()).get();
        // [핵심] Order 상태가 PENDING에서 COMPLETED로 변경되었는지?
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);

        // 3. [DB 검증 2: Payment] '진짜' H2 DB를 조회
        Payment savedPayment = paymentRepository.findByOrder(updatedOrder)
                .orElseThrow(() -> new AssertionError("Payment가 DB에 저장되지 않았습니다."));

        // [핵심] Payment가 tossResponse 내용으로 저장되었는지?
        assertThat(savedPayment.getPgTransactionId()).isEqualTo("pk_test_payment_123");
        assertThat(savedPayment.getPaymentStatus()).isEqualTo("DONE");
        assertThat(savedPayment.getAmount()).isEqualTo(savedPendingOrder.getTotalAmount());

        // 4. [DB 검증 3: Cart (Side Effect)] '진짜' H2 DB를 조회
        List<CartResponse> cartItems = cartRepository.findNextCartItems(userId, null, 10);

        // [핵심] 장바구니가 비워졌는지?
        assertThat(cartItems).isEmpty();
    }

    @Test
    @DisplayName("confirmPayment 통합 테스트: API 실패 시 DB의 Order 상태를 FAILED로 변경한다.")
    void confirmPayment_IntegrationTest_ApiFail() {

        // --- GIVEN (준비) ---
        // 1. '입력값' DTO 생성 (@BeforeEach의 savedPendingOrder 정보와 일치)
        PaymentConfirmRequest dto = new PaymentConfirmRequest(
                "pk_test_payment_fail", // (실패용 paymentKey)
                savedPendingOrder.getOrderNumber(), // 👈 @BeforeEach에서 저장한 PENDING 주문
                savedPendingOrder.getTotalAmount()
        );
        Long userId = savedUser.getId();

        // 2. [핵심] @MockBean이 "Toss API 통신 실패" 예외를 던지도록 설정
        given(tossPaymentsAdapter.confirmPaymentToToss(dto))
                .willThrow(new RuntimeException("Toss API 통신 실패"));


        // --- WHEN (실행) ---
        // [핵심] "confirmPayment()를 실행할 때,
        //        Toss API 예외가 발생하는 것을 기대한다."
        // (paymentService.confirmPayment가 예외를 잡아서 다시 던지므로)
        assertThatThrownBy(() -> {
            paymentService.confirmPayment(dto, userId);

            // [THEN 1] '예외 타입' 검증
        }).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Toss API 통신 실패");


        // --- THEN (검증) ---
        // 1. [DB 검증 1: Order] '진짜' H2 DB를 조회
        Order updatedOrder = orderRepository.findById(savedPendingOrder.getId()).get();

        // [핵심] Order 상태가 PENDING에서 FAILED로 변경되었는지?
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.FAILED);

        // 2. [DB 검증 2: Payment]
        // [핵심] Payment 레코드가 생성되지 않았는지?
        Optional<Payment> paymentOptional = paymentRepository.findByOrder(updatedOrder);
        assertThat(paymentOptional).isEmpty();

        // 3. [DB 검증 3: Cart]
        // [핵심] 장바구니가 삭제되지 않고 "그대로" 남아있는지?
        List<CartResponse> cartItems = cartRepository.findNextCartItems(userId, null, 10);
        assertThat(cartItems).hasSize(1); // 👈 @BeforeEach에서 넣은 1개가 그대로 있음
    }

    @Test
    @DisplayName("cancelOrder 통합 테스트: API 성공 시 DB 작업(재고 복구, Order/Payment 상태 변경)이 성공한다.")
    void cancelOrder_IntegrationTest_Success() {

        // --- GIVEN (준비) ---
        // 1. 상수 정의
        Long userId = savedUser.getId();
        Long skuId = savedSku.getId();
        String paymentKey = "pk_test_to_cancel_123";
        String cancelReason = "테스트 취소";
        Long orderQuantity = 2L;

        // 2. '입력값' DTO 생성
        OrderCancelRequest dto = new OrderCancelRequest(cancelReason);

        // 3. [핵심] "결제 완료" 상태의 'Order', 'OrderItem', 'Payment'를 DB에 저장

        // (OrderService가 재고를 차감한 'COMPLETED' 주문을 생성)
        Order completedOrder = Order.builder()
                .user(savedUser)
                .orderNumber("ORD-TO-CANCEL-123")
                .totalAmount(23000L) // (임의의 값)
                .orderStatus(OrderStatus.COMPLETED) // 👈 [핵심] 완료 상태
                .deliveryStatus(DeliveryStatus.DELIVERED) // 👈 [핵심] 완료 상태
                .orderedAt(LocalDateTime.now())
                .build();

        OrderItem completedItem = OrderItem.builder()
                .sku(savedSku)
                .quantity(orderQuantity) // 👈 2개
                .price(10000L)
                .build();

        completedOrder.addOrderItem(completedItem);
        orderRepository.save(completedOrder); // 👈 Order와 OrderItem 저장

        // (결제 완료된 Payment 레코드 저장)
        Payment completedPayment = Payment.builder()
                .order(completedOrder)
                .pgTransactionId(paymentKey) // 👈 [핵심] 취소할 결제 키
                .amount(23000L)
                .method("카드")
                .paymentStatus("DONE") // 👈 [핵심] 완료 상태
                .paidAt(LocalDateTime.now())
                .build();
        paymentRepository.save(completedPayment);

        // 4. '가짜' Toss 응답 데이터 생성
        Map<String, Object> tossCancelResponse = Map.of(
                "status", "CANCELED"
        );

        // 5. [핵심] @MockBean으로 등록된 '가짜' 어댑터의 행동 정의
        // "tossPaymentsAdapter.cancelPaymentToToss가 호출되면,
        //  (실제 API 호출 대신) '가짜 취소 성공 응답'을 반환해라."
        given(tossPaymentsAdapter.cancelPaymentToToss(paymentKey, cancelReason))
                .willReturn(tossCancelResponse);


        // --- WHEN (실행) ---
        // [핵심] "진짜" PaymentService의 "진짜" cancelOrder 메서드를 호출
        paymentService.cancelOrder(completedOrder.getId(), userId, dto);


        // --- THEN (검증) ---
        // 1. [DB 검증 1: Order] '진짜' H2 DB를 조회
        Order updatedOrder = orderRepository.findById(completedOrder.getId()).get();
        // [핵심] Order 상태가 FAILED / CANCELLED로 변경되었는지?
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(updatedOrder.getDeliveryStatus()).isEqualTo(DeliveryStatus.CANCELLED);

        // 2. [DB 검증 2: Payment] '진짜' H2 DB를 조회
        Payment updatedPayment = paymentRepository.findByOrder(updatedOrder).get();
        // [핵심] Payment 상태가 CANCELED로 변경되었는지?
        assertThat(updatedPayment.getPaymentStatus()).isEqualTo("CANCELED");

        // 3. [DB 검증 3: Stock (Side Effect)] '진짜' H2 DB를 조회
        SkuEntity updatedSku = skuJPARepository.findById(skuId).get();
        // [핵심] 재고가 (INITIAL_STOCK + 2)로 "복구"되었는지 검증
        assertThat(updatedSku.getStockQuantity()).isEqualTo(INITIAL_STOCK + orderQuantity);
    }
}