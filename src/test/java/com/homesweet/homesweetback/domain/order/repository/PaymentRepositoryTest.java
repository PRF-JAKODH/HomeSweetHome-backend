package com.homesweet.homesweetback.domain.order.repository;

// --- Imports ---
import com.homesweet.homesweetback.common.config.QueryDslConfig;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.Payment;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.product.category.repository.impl.ProductCategoryRepositoryImpl;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.ProductCategoryJPARepository;
import com.homesweet.homesweetback.domain.product.category.repository.mapper.ProductCategoryMapper;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.ProductJPARepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.SkuJPARepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime; // 👈 2. [추가] LocalDateTime import
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({QueryDslConfig.class,
        ProductCategoryRepositoryImpl.class,
        ProductCategoryMapper.class})
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository; // 👈 테스트 대상

    @Autowired
    private OrderRepository orderRepository; // 👈 GIVEN에서 Order 저장을 위해 필요

    // --- GIVEN (setUp)에 필요한 의존성 ---
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductCategoryJPARepository productCategoryJPARepository;
    @Autowired
    private ProductJPARepository productJPARepository;
    @Autowired
    private SkuJPARepository skuJPARepository;

    private Order savedOrder; // 👈 테스트에서 사용할 Order

    @BeforeEach
    void setUp() {
        // (SkuJPARepositoryTest와 동일한 GIVEN 데이터 설정)
        User user = User.builder()
                .email("testuser@example.com")
                .name("테스트유저")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User savedUser = userRepository.save(user);

        ProductCategoryEntity category = ProductCategoryEntity.builder()
                .name("테스트 카테고리")
                .depth(0)
                .build();
        ProductCategoryEntity savedCategoryEntity = productCategoryJPARepository.save(category);

        ProductEntity product = ProductEntity.builder()
                .name("테스트 상품")
                .basePrice(10000)
                .discountRate(BigDecimal.ZERO)
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
                .priceAdjustment(0)
                .stockQuantity(100L)
                .build();
        SkuEntity savedSku = skuJPARepository.save(sku);

        // [핵심] Order를 미리 저장해 둠
        Order order = Order.builder()
                .user(savedUser)
                .orderNumber("ORD-PAYMENT-TEST")
                .totalAmount(13000L)
                .orderStatus(OrderStatus.COMPLETED)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .orderedAt(LocalDateTime.now())
                .build();
        savedOrder = orderRepository.save(order);
    }

    @Test
    @DisplayName("findByOrder 쿼리가 Order 객체로 Payment를 정확히 조회한다.")
    void findByOrder_Success() {

        // --- GIVEN ---
        // (@BeforeEach에서 'savedOrder'를 H2 DB에 저장함)
        String targetPaymentKey = "pk_test_payment_123";

        Payment newPayment = Payment.builder()
                .order(savedOrder) // 👈 @BeforeEach에서 저장한 Order
                .pgTransactionId(targetPaymentKey)
                .amount(13000L)
                .method("카드")
                .paymentStatus("DONE")
                .paidAt(LocalDateTime.now())
                .build();

        paymentRepository.save(newPayment); // "진짜" H2 DB에 Payment 저장

        // --- WHEN ---
        // [핵심] findByOrder 메서드를 "진짜" 호출
        Optional<Payment> foundPaymentOptional = paymentRepository.findByOrder(savedOrder);

        // --- THEN ---
        // 1. 조회가 성공했는지?
        assertThat(foundPaymentOptional).isPresent();

        // 2. 조회된 Payment의 pgTransactionId가 GIVEN에서 저장한 값과 일치하는지?
        assertThat(foundPaymentOptional.get().getPgTransactionId()).isEqualTo(targetPaymentKey);

        // 3. 조회된 Payment가 올바른 Order와 연결되어 있는지?
        assertThat(foundPaymentOptional.get().getOrder().getId()).isEqualTo(savedOrder.getId());
    }
}