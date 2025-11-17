package com.homesweet.homesweetback.domain.order.service;
// 아 동시성 테스트 렛츠고
import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.order.dto.request.CreateOrderRequest;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.ProductCategoryJPARepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.ProductJPARepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.SkuJPARepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private SkuJPARepository skuJPARepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductCategoryJPARepository productCategoryRepository;
    @Autowired
    private ProductJPARepository productRepository;

    private Long savedSkuId;
    private Long savedUserId;

    // [핵심] 재고 100개
    private final long INITIAL_STOCK = 100L;

    @BeforeEach
    void setUp() {
        // 1. 데이터 초기화 (기존 데이터 삭제)
        orderRepository.deleteAll();
        skuJPARepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        // 2. 유저 생성
        User user = User.builder()
                .email("test@concurrent.com")
                .name("동시성테스터")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        savedUserId = userRepository.save(user).getId();

        // 3. 상품 및 재고 생성 (재고 100개)
        ProductCategoryEntity category = productCategoryRepository.save(ProductCategoryEntity.builder().name("테스트").depth(0).build());
        ProductEntity product = productRepository.save(ProductEntity.builder()
                .name("인기상품")
                .basePrice(1000)
                .discountRate(BigDecimal.ZERO)
                .shippingPrice(0)
                .brand("브랜드")
                .category(category)
                .seller(user)
                .imageUrl("img")
                .status(ProductStatus.ON_SALE)
                .build());

        SkuEntity sku = SkuEntity.builder()
                .product(product)
                .stockQuantity(INITIAL_STOCK) // 👈 재고 100개 설정
                .priceAdjustment(0)
                .build();
        savedSkuId = skuJPARepository.save(sku).getId();
    }

    @Test
    @DisplayName("동시성 테스트: 100명이 동시에 1개씩 주문하면, 재고는 정확히 0개가 되어야 한다.")
    void createOrder_Concurrency_100Request() throws InterruptedException {
        // --- GIVEN ---
        int threadCount = 100; // 100명의 동시 사용자
        // 멀티 스레드 환경을 만들어주는 ExecutorService (32개 스레드 풀)
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        // 100개의 요청이 끝날 때까지 기다리게 해주는 빗장(Latch)
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 성공/실패 횟수 카운터 (동시성 안전한 AtomicInteger 사용)
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // 주문 요청 DTO (1개 주문)
        CreateOrderRequest.OrderItemRequest itemReq = new CreateOrderRequest.OrderItemRequest(savedSkuId, 1);
        CreateOrderRequest request = new CreateOrderRequest(List.of(itemReq));


        // --- WHEN (동시 실행) ---
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 주문 생성 시도
                    orderService.createOrder(request, savedUserId);
                    successCount.getAndIncrement(); // 성공 카운트 +1
                } catch (Exception e) {
                    // 재고 부족 등으로 실패 시
                    failCount.getAndIncrement(); // 실패 카운트 +1
                    System.out.println("주문 실패: " + e.getMessage());
                } finally {
                    latch.countDown(); // 작업 완료 신호
                }
            });
        }

        // 모든 스레드가 끝날 때까지 대기
        latch.await();


        // --- THEN (검증) ---
        // 1. DB의 최종 재고 조회
        SkuEntity finalSku = skuJPARepository.findById(savedSkuId).orElseThrow();

        // [핵심 검증]
        // 초기 재고 100개 - (성공 횟수 * 1개) = 남은 재고
        // 100명이 1개씩 주문했고, 재고도 100개였으니, 남은 재고는 무조건 0이어야 함.
        // 만약 락이 동작하지 않았다면? -> 재고가 -가 되거나, 0보다 큰 이상한 값이 남음 (Race Condition)
        long expectedStock = INITIAL_STOCK - (successCount.get() * 1);

        System.out.println("성공 횟수: " + successCount.get());
        System.out.println("실패 횟수: " + failCount.get());
        System.out.println("남은 재고: " + finalSku.getStockQuantity());

        assertThat(finalSku.getStockQuantity()).isEqualTo(0L);
        assertThat(successCount.get()).isEqualTo(100); // 100번 다 성공해야 함
    }
}