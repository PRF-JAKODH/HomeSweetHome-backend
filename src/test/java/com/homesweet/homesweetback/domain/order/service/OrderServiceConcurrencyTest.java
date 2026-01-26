//package com.homesweet.homesweetback.domain.order.service;
//
//import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
//import com.homesweet.homesweetback.domain.auth.entity.User;
//import com.homesweet.homesweetback.domain.auth.entity.UserRole;
//import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
//import com.homesweet.homesweetback.domain.order.dto.request.CreateOrderRequest;
//import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
//import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
//import com.homesweet.homesweetback.domain.product.category.repository.jpa.ProductCategoryJPARepository;
//import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.ProductJPARepository;
//import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.SkuJPARepository;
//import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.ProductEntity;
//import com.homesweet.homesweetback.domain.product.product.command.domain.ProductStatus;
//import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.SkuEntity;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest
//@ActiveProfiles("test")
//class OrderServiceConcurrencyTest {
//
//    @Autowired
//    private OrderService orderService;
//
//    @Autowired
//    private RedisStockService redisStockService;
//
//    @Autowired
//    private SkuJPARepository skuJPARepository;
//    @Autowired
//    private UserRepository userRepository;
//    @Autowired
//    private OrderRepository orderRepository;
//    @Autowired
//    private ProductCategoryJPARepository productCategoryRepository;
//    @Autowired
//    private ProductJPARepository productRepository;
//
//    private Long savedSkuId;
//    private Long savedUserId;
//
//    // [핵심] 재고 100개
//    private final long INITIAL_STOCK = 100L;
//
//    @BeforeEach
//    void setUp() {
//        // 1. 데이터 초기화 (기존 데이터 삭제)
//        orderRepository.deleteAll();
//        skuJPARepository.deleteAll();
//        productRepository.deleteAll();
//        userRepository.deleteAll();
//
//        // 2. 유저 생성
//        User user = User.builder()
//                .email("test@concurrent.com")
//                .name("동시성테스터")
//                .provider(OAuth2Provider.GOOGLE)
//                .role(UserRole.USER)
//                .build();
//        savedUserId = userRepository.save(user).getId();
//
//        // 3. 상품 및 재고 생성 (재고 100개)
//        ProductCategoryEntity category = productCategoryRepository.save(ProductCategoryEntity.builder().name("테스트").depth(0).build());
//        ProductEntity product = productRepository.save(ProductEntity.builder()
//                .name("인기상품")
//                .basePrice(1000)
//                .discountRate(BigDecimal.ZERO)
//                .shippingPrice(0)
//                .brand("브랜드")
//                .category(category)
//                .seller(user)
//                .imageUrl("img")
//                .status(ProductStatus.ON_SALE)
//                .build());
//
//        SkuEntity sku = SkuEntity.builder()
//                .product(product)
//                .stockQuantity(INITIAL_STOCK) // 👈 재고 100개 설정
//                .priceAdjustment(0)
//                .build();
//        savedSkuId = skuJPARepository.save(sku).getId();
//
//        redisStockService.setStock(savedSkuId, INITIAL_STOCK);
//
//
//    }
//
//    @Test
//    @DisplayName("동시성 테스트: 100명이 동시에 1개씩 주문하면, 재고는 정확히 0개가 되어야 한다.")
//    void createOrder_Concurrency_100Request() throws InterruptedException {
//        // --- GIVEN ---
//        int threadCount = 100; // 100명의 동시 사용자
//        // 멀티 스레드 환경을 만들어주는 ExecutorService (32개 스레드 풀)
//        ExecutorService executorService = Executors.newFixedThreadPool(32);
//        // 100개의 요청이 끝날 때까지 기다리게 해주는 빗장(Latch)
//        CountDownLatch latch = new CountDownLatch(threadCount);
//
//        // 성공/실패 횟수 카운터 (동시성 안전한 AtomicInteger 사용)
//        AtomicInteger successCount = new AtomicInteger();
//        AtomicInteger failCount = new AtomicInteger();
//
//        // 주문 요청 DTO (1개 주문)
//        CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest(savedSkuId, 1);
//        CreateOrderRequest dto = new CreateOrderRequest(
//                List.of(itemRequest),
//                "테스트수령인",
//                "010-0000-0000",
//                "테스트주소",
//                "요청사항"
//        );
//
//        redisStockService.setStock(savedSkuId, 100L);
//
//        // --- WHEN (동시 실행) ---
//        for (int i = 0; i < threadCount; i++) {
//            executorService.submit(() -> {
//                try {
//                    // 주문 생성 시도
//                    orderService.createOrder(dto, savedUserId);
//                    successCount.getAndIncrement(); // 성공 카운트 +1
//                } catch (Exception e) {
//                    // 재고 부족 등으로 실패 시
//                    failCount.getAndIncrement(); // 실패 카운트 +1
//                    System.out.println("주문 실패: " + e.getMessage());
//                } finally {
//                    latch.countDown(); // 작업 완료 신호
//                }
//            });
//        }
//
//        // 모든 스레드가 끝날 때까지 대기
//        latch.await();
//
//
//        // --- THEN (검증) ---
//        // 1. DB의 최종 재고 조회
//        Long finalRedisStock = redisStockService.getStock(savedSkuId);
//        // [핵심 검증]
//        // 초기 재고 100개 - (성공 횟수 * 1개) = 남은 재고
//        // 100명이 1개씩 주문했고, 재고도 100개였으니, 남은 재고는 무조건 0이어야 함.
//        // 만약 락이 동작하지 않았다면? -> 재고가 -가 되거나, 0보다 큰 이상한 값이 남음 (Race Condition)
//
//        System.out.println("성공 횟수: " + successCount.get());
//        System.out.println("실패 횟수: " + failCount.get());
//        System.out.println("남은 재고(Redis): " + finalRedisStock);
//
//        assertThat(finalRedisStock).isEqualTo(0L);
//        assertThat(successCount.get()).isEqualTo(100); // 100번 다 성공해야 함
//    }
//
//    @Test
//    @DisplayName("시나리오 9: (동시성) 주문 진행 중에 판매자가 상품을 '판매 중지'로 변경하면, 둘 중 하나는 차단되거나 순서대로 처리되어야 한다.")
//    void concurrency_Order_vs_ProductSuspend() throws InterruptedException {
//        // --- GIVEN ---
//        int threadCount = 2; // 구매자 1명 vs 판매자 1명
//        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
//        CountDownLatch latch = new CountDownLatch(threadCount);
//
//        AtomicInteger successOrderCount = new AtomicInteger();
//        AtomicInteger failOrderCount = new AtomicInteger();
//
//        // 주문 요청 DTO
//        CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest(savedSkuId, 1);
//        CreateOrderRequest dto = new CreateOrderRequest(
//                List.of(itemRequest),
//                "테스트수령인",
//                "010-0000-0000",
//                "테스트주소",
//                "요청사항"
//        );
//        // --- WHEN (동시 실행) ---
//
//        // 1. 구매자 스레드 (주문 시도)
//        executorService.submit(() -> {
//            try {
//                // (락 경쟁을 유발하기 위해 아주 약간의 딜레이를 줄 수도 있음)
//                orderService.createOrder(dto, savedUserId);
//                successOrderCount.getAndIncrement();
//            } catch (Exception e) {
//                failOrderCount.getAndIncrement();
//                System.out.println("구매 실패(예상됨): " + e.getMessage());
//            } finally {
//                latch.countDown();
//            }
//        });
//
//        // 2. 판매자 스레드 (판매 중지 시도)
//        executorService.submit(() -> {
//            try {
//                // (주의: 별도의 트랜잭션으로 실행되어야 함. 여기서는 간단히 Repository 사용)
//                // 실제로는 ProductService.suspendProduct() 등을 호출해야 함.
//                // 락 테스트를 위해 직접 DB 업데이트 시도
//                changeProductStatusToSuspended(savedSkuId);
//            } catch (Exception e) {
//                System.out.println("판매 중지 실패: " + e.getMessage());
//            } finally {
//                latch.countDown();
//            }
//        });
//
//        latch.await(); // 두 스레드가 끝날 때까지 대기
//
//        // --- THEN ---
//        // 검증: 데이터가 꼬이지 않았는지 확인
//
//        // 1. 최종 상품 상태 확인
//        transactionTemplate.execute(status -> {
//            SkuEntity sku = skuJPARepository.findById(savedSkuId).orElseThrow();
//            ProductEntity product = sku.getProduct();
//
//            // 트랜잭션 안이므로 Proxy 초기화(DB 조회)가 가능함
//            System.out.println("최종 상품 상태: " + product.getStatus());
//            return null;
//        });
//
//        // [핵심 검증 논리]
//        // Case A: 주문이 먼저 Lock을 잡고 완료됨 -> 그 뒤에 판매 중지됨
//        //         결과: 주문 성공(1) & 상품 상태 SUSPENDED & 재고 차감됨
//
//        // Case B: 판매 중지가 먼저 Lock을 잡고 완료됨 -> 그 뒤에 주문 시도
//        //         결과: 주문 실패(0) & 상품 상태 SUSPENDED & 재고 유지됨
//
//        // 즉, "주문은 성공했는데 상품 상태는 ON_SALE로 남아있다"거나 하는 이상한 상태만 아니면 됨.
//        // 여기서는 락이 걸려있는지 확인하기 위해,
//        // "Product에도 락을 걸어야 한다"는 강사님 말씀을 구현하기 전/후를 비교해야 함.
//    }
//
//    // (테스트를 위한 헬퍼 메서드)
//    // 별도 트랜잭션에서 동작하도록 설정
//    // ProductService가 없으므로 임시로 만듦
//    @Autowired
//    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;
//
//    private void changeProductStatusToSuspended(Long skuId) {
//        transactionTemplate.execute(status -> {
//            SkuEntity sku = skuJPARepository.findById(skuId).orElseThrow();
//            ProductEntity product = sku.getProduct();
//
//            // 여기서도 Product에 락을 걸어야 동시성 제어가 됨 (지금은 락 없음)
//            // product.setStatus(ProductStatus.SUSPENDED);
//            // -> JPA Dirty Checking으로 업데이트
//
//            // (테스트를 위해 강제로 업데이트 쿼리 날림)
//            productRepository.save(product.toBuilder().status(ProductStatus.SUSPENDED).build());
//            return null;
//        });
//    }
//}