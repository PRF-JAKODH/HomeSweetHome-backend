package com.homesweet.homesweetback.domain.order.service;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.order.dto.internal.PendingOrder;
import com.homesweet.homesweetback.domain.order.dto.request.CreateOrderRequest;
import com.homesweet.homesweetback.domain.order.dto.response.OrderReadyResponse;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.ProductCategoryJPARepository;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.ProductJPARepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.SkuJPARepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.command.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.SkuEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // (Spring Boot 3.4+)
// import org.springframework.boot.test.mock.mockito.MockBean; // (Spring Boot 3.3 이하)
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductCategoryJPARepository productCategoryJPARepository;
    @Autowired
    private ProductJPARepository productJPARepository;
    @Autowired
    private SkuJPARepository skuJPARepository;

    // [핵심] Redis 서비스는 Mocking하여 '호출 여부'만 검증합니다.
    // (실제 Redis까지 테스트하려면 @Autowired 하고, docker redis가 켜져 있어야 합니다)
    @MockitoBean
    private RedisStockService redisStockService;

    private User savedUser;
    private SkuEntity savedSku;
    private final long INITIAL_STOCK = 10L;

    @BeforeEach
    void setUp() {
        // (@DataJpaTest와 동일하게, 테스트 실행 전 H2 DB에 GIVEN 데이터 저장)
        User user = User.builder()
                .email("testuser@example.com")
                .name("테스트유저")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        savedUser = userRepository.save(user);

        ProductCategoryEntity category = productCategoryJPARepository.save(ProductCategoryEntity.builder().name("테스트").depth(0).build());

        ProductEntity product = productJPARepository.save(ProductEntity.builder()
                .name("테스트 상품")
                .basePrice(10000)
                .discountRate(new BigDecimal("10.00"))
                .shippingPrice(3000)
                .brand("브랜드")
                .category(category)
                .seller(savedUser)
                .imageUrl("img")
                .status(ProductStatus.ON_SALE)
                .build());

        SkuEntity sku = SkuEntity.builder()
                .product(product)
                .priceAdjustment(1000)
                .stockQuantity(INITIAL_STOCK)
                .build();
        savedSku = skuJPARepository.save(sku);
    }

    @Test
    @DisplayName("createOrder 통합 테스트: DB 저장 없이 Redis 서비스에게 주문 정보를 전달한다.")
    void createOrder_IntegrationTest_Success() {

        // --- GIVEN ---
        int quantityToOrder = 2;
        long expectedTotalAmount = 23000L;

        // [수정] DTO 생성자 (5개 인자)
        CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest(savedSku.getId(), quantityToOrder);
        CreateOrderRequest dto = new CreateOrderRequest(
                List.of(itemRequest),
                "테스트수령인",
                "010-0000-0000",
                "테스트주소",
                "문앞"
        );

        Long userId = savedUser.getId();

        // [Stubbing] Redis 서비스는 아무 일도 하지 않음 (에러만 안 나면 됨)
        doNothing().when(redisStockService).decreaseStock(anyLong(), anyLong());
        doNothing().when(redisStockService).pushPendingOrder(any(PendingOrder.class));
        doNothing().when(redisStockService).cacheOrder(any(PendingOrder.class));


        // --- WHEN ---
        OrderReadyResponse response = orderService.createOrder(dto, userId);


        // --- THEN ---
        // 1. 결과 검증 (가짜 응답값 확인)
        assertThat(response.orderNumber()).isNotNull();

        // 2. [핵심] Redis 서비스가 호출되었는지 검증
        verify(redisStockService).pushPendingOrder(any(PendingOrder.class));
        verify(redisStockService).cacheOrder(any(PendingOrder.class));

        // 3. [중요] DB에는 아직 저장되지 않았어야 함! (Write-Behind)
        Optional<Order> orderInDb = orderRepository.findByOrderNumber(response.orderNumber());
        assertThat(orderInDb).isEmpty(); // DB에 없어야 성공

        // 4. [중요] DB 재고도 차감되지 않았어야 함! (Redis만 차감했으니까)
        SkuEntity skuInDb = skuJPARepository.findById(savedSku.getId()).get();
        assertThat(skuInDb.getStockQuantity()).isEqualTo(INITIAL_STOCK); // 그대로 10개여야 함
    }
}