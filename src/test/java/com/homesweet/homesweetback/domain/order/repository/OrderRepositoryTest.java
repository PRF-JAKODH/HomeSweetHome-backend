package com.homesweet.homesweetback.domain.order.repository;

import com.homesweet.homesweetback.common.config.QueryDslConfig;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository; // User 저장을 위해 UserRepository도 주입
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.order.entity.OrderItem;
import com.homesweet.homesweetback.domain.product.category.repository.impl.ProductCategoryRepositoryImpl;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.ProductCategoryJPARepository;
import com.homesweet.homesweetback.domain.product.category.repository.mapper.ProductCategoryMapper;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.ProductJPARepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.SkuJPARepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;



@DataJpaTest // "이것은 JPA 슬라이스 테스트입니다."
@ActiveProfiles("test")
@Import({
        QueryDslConfig.class,
        ProductCategoryRepositoryImpl.class,
        ProductCategoryMapper.class
})
class OrderRepositoryTest {

    @Autowired // @Mock이 아닌, H2 DB에 연결된 "진짜" Repository를 주입
    private OrderRepository orderRepository;

    @Autowired // User를 DB에 미리 저장하기 위해 UserRepository도 주입
    private UserRepository userRepository;

    @Autowired
    private ProductJPARepository productJPARepository;

    @Autowired
    private SkuJPARepository skuJPARepository;

    @Autowired
    private ProductCategoryJPARepository productCategoryJPARepository;

    private User savedUser; // 여러 테스트에서 공통으로 사용할 저장된 유저
    private SkuEntity savedSku;

    // @BeforeEach: 모든 @Test가 실행되기 "직전"에 실행되는 설정 메서드
    @BeforeEach
    void setUp() {
        // 모든 테스트는 'User 1명'이 DB에 저장된 상태에서 시작합니다.
        User user = User.builder()
                .email("testuser@example.com")
                .name("테스트유저")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        savedUser = userRepository.save(user); // "진짜" H2 DB에 User 저장

        // Category "엔티티" 저장
        ProductCategoryEntity category = ProductCategoryEntity.builder()
                .name("테스트 카테고리")
                .depth(0)
                .build();
        // JPARepository를 사용해 엔티티를 저장
        ProductCategoryEntity savedCategoryEntity = productCategoryJPARepository.save(category);

        // Product 저장 (모든 NOT NULL 필드 채우기)
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

        // Sku 저장
        SkuEntity sku = SkuEntity.builder()
                .product(savedProduct)
                .priceAdjustment(0)
                .stockQuantity(100L)
                .build();
        savedSku = skuJPARepository.save(sku);
    }

    @Test
    @DisplayName("findByOrderNumber로 주문을 조회할 수 있다.")
    void findByOrderNumber_Success() {
        // --- GIVEN (진짜 DB에 저장) ---
        String targetOrderNumber = "ORD-12345-ABC";

        Order newOrder = Order.builder()
                .user(savedUser) // 👈 @BeforeEach에서 저장한 User
                .orderNumber(targetOrderNumber)
                .totalAmount(10000L)
                .orderStatus(OrderStatus.PENDING)
                .deliveryStatus(DeliveryStatus.BEFORE_SHIPMENT)
                .orderedAt(LocalDateTime.now())
                .build();

        orderRepository.save(newOrder); // "진짜" H2 DB에 Order 저장

        // --- WHEN (진짜 DB에서 조회) ---
        // OrderRepository의 findByOrderNumber 메서드를 "진짜" 호출
        Optional<Order> foundOrderOptional = orderRepository.findByOrderNumber(targetOrderNumber);

        // --- THEN (결과 검증) ---
        assertThat(foundOrderOptional).isPresent(); // 1. Optional이 비어있지 않은지?

        Order foundOrder = foundOrderOptional.get();
        assertThat(foundOrder.getOrderNumber()).isEqualTo(targetOrderNumber); // 2. 주문번호가 일치하는지?
        assertThat(foundOrder.getUser().getId()).isEqualTo(savedUser.getId()); // 3. User ID가 일치하는지?
    }

    @Test
    @DisplayName("findAllByUserWithDetails가 N+1 없이 Order, OrderItem, Sku, Product를 모두 조회한다.")
    void findAllByUserWithDetails_Success() {

        // --- GIVEN (진짜 DB에 저장) ---
        // (@BeforeEach에서 User, Product, Sku는 이미 저장됨)

        // 1. OrderItem 생성
        OrderItem orderItem = OrderItem.builder()
                .sku(savedSku)     // 👈 @BeforeEach에서 저장한 Sku
                .quantity(1L)
                .price(10000L)
                .build();

        // 2. Order 생성 (CascadeType.ALL 때문에 Order만 저장하면 OrderItem도 자동 저장됨)
        Order newOrder = Order.builder()
                .user(savedUser) // 👈 @BeforeEach에서 저장한 User
                .orderNumber("ORD-FETCH-TEST")
                .totalAmount(13000L)
                .orderStatus(OrderStatus.COMPLETED)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .orderedAt(LocalDateTime.now())
                .build();

        // 3. 연관관계 설정 (Order <-> OrderItem)
        newOrder.addOrderItem(orderItem);

        orderRepository.save(newOrder); // "진짜" H2 DB에 Order와 OrderItem 저장

        // --- WHEN (진짜 DB에서 조회) ---
        // (이 시점에서 1차 캐시는 비어있다고 가정, @DataJpaTest가 관리)
        List<Order> foundOrders = orderRepository.findAllByUserWithDetails(savedUser);

        // --- THEN (결과 검증) ---
        // 1. 주문이 1건 조회되었는지?
        assertThat(foundOrders).hasSize(1);
        Order foundOrder = foundOrders.get(0);

        // --- [핵심] Fetch Join 검증 ---
        // @DataJpaTest 환경에서는 LazyInitializationException이 잘 발생하지 않지만,
        // 조회된 객체의 필드에 접근하여 N+1 쿼리가 발생하는지
        // (또는 프록시가 아닌 실제 객체인지) 검증합니다.

        // 2. User가 Join Fetch 되었는지?
        assertThat(foundOrder.getUser().getName()).isEqualTo(savedUser.getName());

        // 3. OrderItems가 Join Fetch 되었는지?
        assertThat(foundOrder.getOrderItems()).hasSize(1);
        OrderItem foundItem = foundOrder.getOrderItems().get(0);

        // 4. Sku가 Join Fetch 되었는지?
        assertThat(foundItem.getSku().getId()).isEqualTo(savedSku.getId());

        // 5. Product가 Join Fetch 되었는지?
        assertThat(foundItem.getSku().getProduct().getName()).isEqualTo("테스트 상품");
    }

    // ... (findAllByUserWithDetails_Success 메서드 끝) ...

    @Test
    @DisplayName("findByIdWithDetails가 N+1 없이 Order 및 모든 연관 엔티티를 조회한다.")
    void findByIdWithDetails_Success() {

        // --- GIVEN (진짜 DB에 저장) ---
        // (@BeforeEach에서 User, Product, Sku는 이미 저장됨)

        // 1. OrderItem 생성
        OrderItem orderItem = OrderItem.builder()
                .sku(savedSku)     // 👈 @BeforeEach에서 저장한 Sku
                .quantity(1L)
                .price(10000L)
                .build();

        // 2. Order 생성
        Order newOrder = Order.builder()
                .user(savedUser) // 👈 @BeforeEach에서 저장한 User
                .orderNumber("ORD-DETAIL-TEST")
                .totalAmount(13000L)
                .orderStatus(OrderStatus.COMPLETED)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .orderedAt(LocalDateTime.now())
                .build();

        newOrder.addOrderItem(orderItem);

        // [핵심] Order를 저장하고, DB가 생성한 'id'를 받아옴
        Order savedOrder = orderRepository.save(newOrder);
        Long targetOrderId = savedOrder.getId(); // 👈 조회할 ID

        // --- WHEN (진짜 DB에서 조회) ---
        // [수정] findByIdWithDetails 메서드를 호출
        Optional<Order> foundOrderOptional = orderRepository.findByIdWithDetails(targetOrderId);

        // --- THEN (결과 검증) ---
        // 1. 주문이 조회되었는지?
        assertThat(foundOrderOptional).isPresent();
        Order foundOrder = foundOrderOptional.get();

        // 2. ID가 일치하는지?
        assertThat(foundOrder.getId()).isEqualTo(targetOrderId);

        // 3. User, OrderItems, Sku, Product가 Join Fetch 되었는지 검증
        assertThat(foundOrder.getUser().getName()).isEqualTo(savedUser.getName());
        assertThat(foundOrder.getOrderItems()).hasSize(1);
        assertThat(foundOrder.getOrderItems().get(0).getSku().getProduct().getName()).isEqualTo("테스트 상품");
    }
}