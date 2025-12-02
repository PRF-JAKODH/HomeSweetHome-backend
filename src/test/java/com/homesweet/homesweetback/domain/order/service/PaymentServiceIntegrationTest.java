package com.homesweet.homesweetback.domain.order.service;

// (필요한 import문들)
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.order.adapter.TossPaymentsAdapter;
import com.homesweet.homesweetback.domain.order.dto.internal.PendingPayment;
import com.homesweet.homesweetback.domain.order.dto.request.OrderCancelRequest;
import com.homesweet.homesweetback.domain.order.dto.request.PaymentConfirmRequest;
import com.homesweet.homesweetback.domain.order.dto.response.PaymentConfirmResponse;
import com.homesweet.homesweetback.domain.order.entity.*;
import com.homesweet.homesweetback.domain.order.repository.OrderItemRepository;
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

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceIntegrationTest {

    // --- 4. [수정] "진짜" Service와 Repository 주입 ---
    @Autowired
    private OrderItemRepository orderItemRepository;
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
    @Autowired
    EntityManager entityManager; // 테스트 클래스에 주입 필요

    // --- 5. [핵심] "가짜" Adapter Bean 주입 ---
    @MockitoBean // 👈 @Mock이 아닌 @MockBean
    private TossPaymentsAdapter tossPaymentsAdapter;

    @MockitoBean
    private RedisStockService redisStockService;

    // (테스트 간 데이터를 공유할 필드)
    private User savedUser;
    private SkuEntity savedSku;
    private Order savedPendingOrder; // 👈 결제할 'PENDING' 주문
    private final long INITIAL_STOCK = 10L;

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
        cartRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        skuJPARepository.deleteAll();
        productJPARepository.deleteAll();
        productCategoryJPARepository.deleteAll();
        userRepository.deleteAll();
    }

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
        savedPendingOrder = orderRepository.save(order); // 1. Order 먼저 저장
        // (OrderService가 재고를 차감했다고 가정. 여기서는 재고 차감 로직은 테스트하지 않음)

        OrderItem orderItem = OrderItem.builder()
                .sku(savedSku)
                .quantity(1L)
                .price(10000L) // (OrderService가 계산한 가격)
                .order(savedPendingOrder) // 👈 2. 저장된 Order를 넣어줌
                .build();
        orderItemRepository.save(orderItem);

        savedPendingOrder = orderRepository.findByIdWithDetails(savedPendingOrder.getId()).get();

        System.err.println("저장된 주문의 SKU ID: " + savedPendingOrder.getOrderItems().get(0).getSku().getId());

        // [핵심] PaymentService가 삭제할 '장바구니' 데이터를 미리 저장
        Cart cartItem = Cart.create(savedUser.getId(), savedSku.getId(), 1); // 👈 Sku 1개
        cartRepository.save(cartItem);
    }
    @Test
    @DisplayName("confirmPayment 통합 테스트: API 성공 시 Redis에 결제 정보를 Push하고 장바구니를 비운다.")
    void confirmPayment_IntegrationTest_Success() {

        System.err.println("저장된 주문의 아이템 개수: " + savedPendingOrder.getOrderItems().size());
        if (!savedPendingOrder.getOrderItems().isEmpty()) {
            System.err.println("첫 번째 아이템 SKU ID: " + savedPendingOrder.getOrderItems().get(0).getSku().getId());
        }

        // --- GIVEN ---
        PaymentConfirmRequest dto = new PaymentConfirmRequest(
                "pk_test_payment_123",
                savedPendingOrder.getOrderNumber(),
                savedPendingOrder.getTotalAmount()
        );
        Long userId = savedUser.getId();

        Map<String, Object> tossResponse = Map.of(
                "paymentKey", "pk_test_payment_123",
                "method", "카드",
                "status", "DONE",
                "paidAt", "2025-11-17T10:00:00"
        );

        // [Stubbing] Toss 성공
        given(tossPaymentsAdapter.confirmPaymentToToss(dto)).willReturn(tossResponse);

        // [Stubbing] Redis 동작 (void)
        doNothing().when(redisStockService).pushPendingPayment(any(PendingPayment.class));
        // (DB에 없을 때를 대비한 캐시 조회도 일단 Stubbing 해두면 안전함)
        given(redisStockService.getCachedOrder(anyString())).willReturn(null);


        // --- WHEN ---
        PaymentConfirmResponse response = paymentService.confirmPayment(dto, userId);


        // --- THEN ---
        // 1. [핵심] Redis 서비스가 호출되었는지 검증 (DB 저장 대신)
        verify(redisStockService, times(1)).pushPendingPayment(any(PendingPayment.class));

        List<CartResponse> remainingItems = cartRepository.findNextCartItems(userId, null, 100);
        if (!remainingItems.isEmpty()) {
            Long remainingSkuId = remainingItems.get(0).skuId(); // (record니까 .skuId())
            System.err.println(">>>>>>>> 남은 장바구니 SKU ID: " + remainingSkuId);
            System.err.println(">>>>>>>> 기대했던(주문한) SKU ID: " + savedSku.getId());

            // 만약 두 ID가 다르다면, Order에 들어간 Sku ID가 잘못된 것입니다.
        }

        // 2. [DB 검증: Cart] 장바구니는 여기서 바로 지워지므로 검증 가능
        long cartCount = cartRepository.countByUserId(savedUser.getId()); // (Repository 메서드로 확인)
        assertThat(cartCount).isEqualTo(0); // 비워져야 함
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