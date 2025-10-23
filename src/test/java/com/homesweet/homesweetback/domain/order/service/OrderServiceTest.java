package com.homesweet.homesweetback.domain.order.service;

// (ObjectMapper, RestTemplate 등 기본)
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

// (DTO - API 1)
import com.homesweet.homesweetback.domain.order.dto.request.CreateOrderRequest;
import com.homesweet.homesweetback.domain.order.dto.response.OrderReadyResponse;

// (DTO - API 2)
import com.homesweet.homesweetback.domain.order.dto.request.PaymentConfirmRequest;
import com.homesweet.homesweetback.domain.order.dto.response.PaymentConfirmResponse;

// (Entity)
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.order.entity.Payment;

// (Repository)
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.order.repository.PaymentRepository;

// (Custom Exceptions)
import com.homesweet.homesweetback.common.exception.OrderNotFoundException;
import com.homesweet.homesweetback.common.exception.PaymentMismatchException;

// (JUnit 5 - 테스트 기본)
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

// (Mockito - 모킹)
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*; // (when, times, verify, never)

// (AssertJ - 검증)
import static org.assertj.core.api.Assertions.assertThat;

// (Java Util)
import java.util.List;
import java.util.Map;
import java.util.Optional;

// (Spring Test Util)
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.HttpEntity;

// 1. Mockito 테스트 환경을 사용하겠다고 선언
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    // 2. @Mock: 가짜(Mock) 객체 생성 (우리의 '가짜 농장')
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    // 3. @InjectMocks: '진짜' OrderService를 만들되,
    //    위에서 @Mock으로 만든 가짜 객체들을 이 '진짜' 객체에 "주입"
    @InjectMocks
    private OrderService orderService;

    // (참고) tossSecretKey는 @Value로 주입되므로,
    // 테스트에서는 리플렉션을 쓰거나 private 변수에 값을 직접 할당해야 합니다.
    // (일단 API 1 테스트에서는 secretKey가 필요 없으므로 생략)

    // ▼▼▼▼▼ (추가) ▼▼▼▼▼
    @BeforeEach
    void setUp() {
        // OrderService 객체 내부의 "tossSecretKey"라는 이름의 private 필드에
        // "fake-secret-key"라는 가짜 값을 강제로 주입합니다.
        ReflectionTestUtils.setField(orderService, "tossSecretKey", "fake-secret-key");
    }
    // ▲▲▲▲▲ (추가) ▲▲▲▲▲


    @Test
    @DisplayName("API 1 (주문 생성) - 성공")
    void createOrder_success() {
        // ========== [ 1. Arrange (Given) - 준비 ] ==========

        // 1-1. 테스트용 요청(Request) DTO 생성
        CreateOrderRequest.OrderItemRequest item1 = new CreateOrderRequest.OrderItemRequest(101L, 1);
        CreateOrderRequest.OrderItemRequest item2 = new CreateOrderRequest.OrderItemRequest(102L, 2);

        CreateOrderRequest requestDto = new CreateOrderRequest(
                List.of(item1, item2),
                "테스트 수령인",
                "010-1234-5678",
                "테스트 배송지",
                "문 앞"
        );
        Long userId = 1L;

        // 1-2. (모킹) Mock 객체의 행동 정의 (필요한 경우)
        // (현재 createOrder는 save만 하므로, save의 반환값을 정의할 필요는 없음)
        // (만약 save된 Order 객체를 받아 뭔가를 더 한다면 given/when 구문이 필요)


        // ========== [ 2. Act (When) - 실행 ] ==========

        // 2-1. 테스트 대상 메서드(createOrder) 실행
        OrderReadyResponse response = orderService.createOrder(requestDto, userId);


        // ========== [ 3. Assert (Then) - 검증 ] ==========

        // 3-1. 반환된 DTO(Response)가 예상대로인지 검증
        assertThat(response).isNotNull();
        assertThat(response.merchantUid()).isNotNull(); // UUID는 랜덤이므로 null이 아닌지만 체크

        // 3-2. (중요) Mock 가격 로직 검증 (상품1: 1*10000, 상품2: 2*10000 = 총 30000)
        assertThat(response.totalAmount()).isEqualTo(30000L);

        // 3-3. (중요) Mock 주문 이름 로직 검증 ("상품 101 외 1건")
        assertThat(response.orderName()).isEqualTo("상품 101 외 1건");

        // 3-4. (중요) '가짜' Repository가 우리가 원한 대로 호출되었는지 검증
        // "orderRepository의 save() 메서드가, Order 타입의 그 어떤(any) 객체를 받아서,
        //  정확히 1번(times(1)) 호출되었습니까?"
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("API 2 (결제 검증) - 성공")
    void confirmPayment_success() throws Exception {
        // ========== [ 1. Arrange (Given) - 준비 ] ==========

        // 1-1. 요청 DTO 준비
        String merchantUid = "test-merchant-uid";
        Long amount = 50000L;
        PaymentConfirmRequest requestDto = new PaymentConfirmRequest("test-payment-key", merchantUid, amount);

        // 1-2. (가짜) Order 객체 준비 (DB에서 찾아올)
        Order mockOrder = Order.builder()
                .merchantUid(merchantUid)
                .status(OrderStatus.PENDING) // ★ 상태가 PENDING
                .totalAmount(amount) // ★ 금액이 DTO와 일치
                .userId(1L)
                .recipientName("테스트")
                .recipientPhone("010")
                .shippingAddress("주소")
                .build();

        // 1-3. (가짜) 토스 API 응답(JSON) 준비
        Map<String, Object> tossResponse = Map.of(
                "paymentKey", "test-payment-key",
                "method", "카드",
                "status", "DONE", // ★ 결제 완료
                "paidAt", "2025-10-22T15:00:00+09:00"
        );

        // 1-4. (★모킹★) Repository 및 RestTemplate의 행동 정의

        // "orderRepository.findByMerchantUid()가 호출되면, mockOrder를 Optional로 감싸서 반환해라"
        when(orderRepository.findByMerchantUid(merchantUid)).thenReturn(Optional.of(mockOrder));

        // "restTemplate.postForObject()가 호출되면, tossResponse를 Map으로 반환해라"
        when(restTemplate.postForObject(
                any(String.class), // URL은 아무거나
                any(HttpEntity.class), // HttpEntity도 아무거나
                eq(Map.class) // 반환 타입이 Map인
        )).thenReturn(tossResponse);

        // "objectMapper가 호출되면, JSON 문자열을 반환해라"
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"status\":\"DONE\"}");


        // ========== [ 2. Act (When) - 실행 ] ==========
        PaymentConfirmResponse response = orderService.confirmPayment(requestDto, 1L);


        // ========== [ 3. Assert (Then) - 검증 ] ==========

        // 3-1. 응답 DTO 검증
        assertThat(response.merchantUid()).isEqualTo(merchantUid);
        assertThat(response.status()).isEqualTo("DELIVERED"); // ★ 상태가 DELIVERED로 변경되었는가

        // 3-2. (중요) 가짜 Repository가 올바르게 호출되었는가
        // "paymentRepository.save()가 Payment 타입의 객체를 받아서 1번 호출되었는가?"
        verify(paymentRepository, times(1)).save(any(Payment.class));

        // 3-3. (중요) 엔티티의 상태가 실제로 변경되었는가
        assertThat(mockOrder.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }
    @Test
    @DisplayName("API 2 (결제 검증) - 실패: 주문을 찾을 수 없음")
    void confirmPayment_fail_orderNotFound() {
        // ========== [ 1. Arrange (Given) - 준비 ] ==========
        String merchantUid = "non-existent-uid";
        PaymentConfirmRequest requestDto = new PaymentConfirmRequest("pkey", merchantUid, 50000L);

        // "orderRepository.findByMerchantUid()가 호출되면, '빈 박스'를 반환해라"
        when(orderRepository.findByMerchantUid(merchantUid)).thenReturn(Optional.empty());


        // ========== [ 2. Act (When) & 3. Assert (Then) - 실행 및 검증 ] ==========

        // "orderService.confirmPayment()를 실행하면, OrderNotFoundException이 터지는가?"
        assertThrows(OrderNotFoundException.class, () -> {
            orderService.confirmPayment(requestDto, 1L);
        });

        // "실패했으므로, paymentRepository.save()는 절대 호출되면 안 된다"
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("API 2 (결제 검증) - 실패: 결제 금액 불일치 (위변조)")
    void confirmPayment_fail_mismatchAmount() {
        // ========== [ 1. Arrange (Given) - 준비 ] ==========
        String merchantUid = "test-merchant-uid";
        Long dbAmount = 50000L; // ★ DB에는 50000원
        Long requestAmount = 30000L; // ★ 요청은 30000원

        PaymentConfirmRequest requestDto = new PaymentConfirmRequest("pkey", merchantUid, requestAmount);

        // (가짜) Order 객체 준비 (DB의 금액은 50000원)
        Order mockOrder = Order.builder()
                .merchantUid(merchantUid)
                .status(OrderStatus.PENDING)
                .totalAmount(dbAmount) // ★
                .build();

        // "findByMerchantUid()가 호출되면, 이 mockOrder를 반환해라"
        when(orderRepository.findByMerchantUid(merchantUid)).thenReturn(Optional.of(mockOrder));


        // ========== [ 2. Act (When) & 3. Assert (Then) - 실행 및 검증 ] ==========

        // "실행하면, PaymentMismatchException이 터지는가?"
        assertThrows(PaymentMismatchException.class, () -> {
            orderService.confirmPayment(requestDto, 1L);
        });

        // "실패했으므로, paymentRepository.save()는 절대 호출되면 안 된다"
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}