package com.homesweet.homesweetback.domain.order.service;

// (필요한 import문들)
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.order.dto.request.CreateOrderRequest;
import com.homesweet.homesweetback.domain.order.dto.response.OrderReadyResponse;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
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
import org.springframework.boot.test.context.SpringBootTest; // 👈 1. [핵심] @SpringBootTest
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional; // 👈 2. [핵심] @Transactional

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest // 👈 "이것은 모든 Bean을 로드하는 통합 테스트입니다."
@Transactional  // 👈 3. "모든 테스트 메서드가 끝나면 DB를 '롤백(Rollback)'하세요."
@ActiveProfiles("test") // 👈 4. H2 DB 호환 모드 활성화
class OrderServiceIntegrationTest {

    // 5. [핵심] @Mock이 아닌 "진짜" Service와 Repository들을 주입받습니다.
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

    // (테스트 간 데이터를 공유할 필드)
    private User savedUser;
    private SkuEntity savedSku;
    private final long INITIAL_STOCK = 10L; // 👈 초기 재고

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
    }

    @Test
    @DisplayName("createOrder 통합 테스트: 주문 생성 시 재고 차감 및 DB 저장이 성공적으로 이뤄진다.")
    void createOrder_IntegrationTest_Success() {

        // --- GIVEN (준비) ---
        // 1. 상수 정의
        int quantityToOrder = 2;
        long expectedDiscountedPrice = 10000L; // (10000 * 0.9) + 1000
        long expectedShippingPrice = 3000L;
        long expectedTotalAmount = (expectedDiscountedPrice * quantityToOrder) + expectedShippingPrice; // 23,000L

        // 2. '입력값' DTO 생성 (@BeforeEach에서 저장한 Sku의 ID 사용)
        CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest(savedSku.getId(), quantityToOrder);
        CreateOrderRequest dto = new CreateOrderRequest(List.of(itemRequest));

        // 3. 사용자 ID (@BeforeEach에서 저장한 User의 ID 사용)
        Long userId = savedUser.getId();


        // --- WHEN (실행) ---
        // [핵심] "진짜" OrderService의 "진짜" createOrder 메서드를 호출
        // (이 메서드는 내부적으로 "진짜" Repository와 "진짜" H2 DB와 통신합니다)
        OrderReadyResponse response = orderService.createOrder(dto, userId);


        // --- THEN (검증) ---
        // 1. [DTO 검증] 반환된 DTO의 금액이 올바른지?
        assertThat(response.totalAmount()).isEqualTo(expectedTotalAmount);
        assertThat(response.totalShippingPrice()).isEqualTo(expectedShippingPrice);
        assertThat(response.orderNumber()).isNotNull(); // 주문번호가 생성되었는지?

        // 2. [DB 검증 1: Order] '진짜' H2 DB에서 방금 생성된 Order를 조회
        Order foundOrder = orderRepository.findByOrderNumber(response.orderNumber())
                .orElseThrow(() -> new AssertionError("저장된 Order를 찾을 수 없습니다."));

        assertThat(foundOrder.getTotalAmount()).isEqualTo(expectedTotalAmount);
        assertThat(foundOrder.getUser().getId()).isEqualTo(userId);
        assertThat(foundOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(foundOrder.getOrderItems()).hasSize(1);
        assertThat(foundOrder.getOrderItems().get(0).getPrice()).isEqualTo(expectedDiscountedPrice);

        // 3. [DB 검증 2: Stock (Side Effect)] '진짜' H2 DB에서 Sku 재고를 다시 조회
        SkuEntity updatedSku = skuJPARepository.findById(savedSku.getId())
                .orElseThrow(() -> new AssertionError("Sku를 찾을 수 없습니다."));

        // [핵심] 재고가 (10 - 2 = 8)로 차감되었는지 검증
        assertThat(updatedSku.getStockQuantity()).isEqualTo(INITIAL_STOCK - quantityToOrder);
    }
}
