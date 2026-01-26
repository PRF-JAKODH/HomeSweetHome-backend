package com.homesweet.homesweetback.domain.order.service;

import com.homesweet.homesweetback.common.exception.PaymentMismatchException;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.order.dto.internal.PendingOrder;
import com.homesweet.homesweetback.domain.order.dto.request.OrderCancelRequest;
import com.homesweet.homesweetback.domain.order.dto.request.PaymentConfirmRequest;
import com.homesweet.homesweetback.domain.order.dto.response.PaymentConfirmResponse;
import com.homesweet.homesweetback.domain.order.entity.*;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.order.repository.PaymentRepository;
import com.homesweet.homesweetback.domain.order.adapter.TossPaymentsAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyLong;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TossPaymentsAdapter tossPaymentsAdapter;

    @Mock
    private PaymentProcessor paymentProcessor;

    @Mock
    private RedisStockService redisStockService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("시나리오 1: 결제 성공")
    void confirmPayment() {
        //Given
        Long userId = 1L;
        String orderNumber = "ORD-UUID-12345";
        Long amount = 23000L;
        Long orderDbId = 1L;

        PaymentConfirmRequest dto = new PaymentConfirmRequest("pk_test_12345", orderNumber, amount);

        User fakeUser = User.builder().id(userId).build();
        Order order = Order.builder()
                .id(orderDbId)
                .user(fakeUser)
                .totalAmount(amount)
                .orderStatus(OrderStatus.PENDING)
                .build();
        doReturn(Optional.of(order)).when(orderRepository).findByOrderNumberWithItems(orderNumber);

        Map<String, Object> tossResponse = Map.of("status", "DONE", "paymentKey", "pk_test_12345");
        doReturn(tossResponse).when(tossPaymentsAdapter).confirmPaymentToToss(dto);

        // [추가] paymentProcessor는 void 메서드이므로 doNothing (선택사항이나 명시적으로 좋음)
        doNothing().when(paymentProcessor).processPaymentSuccessDB(any(Order.class), any(Map.class), eq(userId));

        //When
        PaymentConfirmResponse response = paymentService.confirmPayment(dto, userId);

        //Then
        //결과 검증
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(orderDbId);

        //행위 검증
        verify(tossPaymentsAdapter, times(1)).confirmPaymentToToss(dto);
        verify(paymentProcessor, times(1)).processPaymentSuccessDB(order, tossResponse, userId);
    }

    @Test
    @DisplayName("시나리오 2: Toss API 결제 승인 실패 시, 실패 로직(processPaymentFailDB)을 호출한다.")
    void confirmPayment_Fail_TossApiError() {

        // --- GIVEN (주어진 것) ---
        // 1. 상수 정의 (시나리오 1과 유사)
        Long userId = 1L;
        String orderNumber = "ORD-UUID-12345";
        Long amount = 23000L;
        Long orderDbId = 1L;

        // 2. 입력값 DTO 생성
        PaymentConfirmRequest dto = new PaymentConfirmRequest("pk_test_12345", orderNumber, amount);

        // 3. '가짜 엔티티' 생성
        User fakeUser = User.builder().id(userId).build();
        Order order = Order.builder()
                .id(orderDbId)
                .user(fakeUser)
                .totalAmount(amount)
                .orderStatus(OrderStatus.PENDING)
                .build();

        // 4. 'Mock' Repository 행동 정의 (Stubbing)
        // (OrderRepository는 정상적으로 Order를 반환해야 함)
        given(orderRepository.findByOrderNumberWithItems(orderNumber)).willReturn(Optional.of(order));

        // 5. [핵심] Adapter가 RuntimeException을 "발생"시키도록 설정
        // "tossPaymentsAdapter.confirmPaymentToToss(dto)가 호출되면,
        //  RuntimeException을 발생(throw)시켜라."
        given(tossPaymentsAdapter.confirmPaymentToToss(dto))
                .willThrow(new RuntimeException("Toss API 통신 실패"));

        // --- WHEN (실행) & THEN (결과) ---
        // 1. "confirmPayment()를 실행할 때, RuntimeException이 발생하는 것을 기대한다."
        assertThatThrownBy(() -> {
            paymentService.confirmPayment(dto, userId);

            // 2. '예외 타입' 검증
        }).isInstanceOf(RuntimeException.class)
                // 3. '예외 메시지' 검증
                .hasMessageContaining("Toss API 통신 실패");


        // 4. (가장 중요) '행위' 검증

        // "tossPaymentsAdapter.confirmPaymentToToss는 '정확히 1번' 호출되었는가?"
        verify(tossPaymentsAdapter, times(1)).confirmPaymentToToss(dto);

        // "API가 실패했으니, processPaymentFailDB가 '정확히 1번' 호출되었는가?"
        // (doNothing()으로 설정했기 때문에, 호출 여부만 검증 가능)
        verify(paymentProcessor, times(1)).processPaymentFailDB(order);

        // "API가 실패했으니, processPaymentSuccessDB는 '절대' 호출되지 않았는가?"
        verify(paymentProcessor, never()).processPaymentSuccessDB(any(Order.class), any(Map.class), anyLong());
    }

    @Test
    @DisplayName("시나리오 3: DB의 주문 금액과 DTO의 결제 금액이 다르면 PaymentMismatchException이 발생한다.")
    void confirmPayment_Fail_AmountMismatch() {

        // --- GIVEN (주어진 것) ---
        // 1. 상수 정의
        Long userId = 1L;
        String orderNumber = "ORD-UUID-12345";
        Long orderDbId = 1L;

        // [핵심] DB에 저장된 금액과 DTO로 들어온 금액을 "다르게" 설정
        Long dbAmount = 10000L; // DB에는 10000원으로 저장되어 있음
        Long dtoAmount = 9000L;  // 해커가 9000원으로 위변조 시도

        // 2. 입력값 DTO 생성 (위변조된 '9000L' 사용)
        PaymentConfirmRequest dto = new PaymentConfirmRequest("pk_test_12345", orderNumber, dtoAmount);

        // 3. '가짜 엔티티' 생성 (DB 원본 '10000L' 사용)
        User fakeUser = User.builder().id(userId).build();
        Order order = Order.builder()
                .id(orderDbId)
                .user(fakeUser)
                .totalAmount(dbAmount) // DB 원본 금액
                .orderStatus(OrderStatus.PENDING)
                .build();

        // 4. 'Mock' Repository 행동 정의 (Stubbing)
        // OrderRepository는 DB 원본(10000L)이 담긴 order를 정상 반환
        given(orderRepository.findByOrderNumberWithItems(orderNumber)).willReturn(Optional.of(order));

        // (이 테스트에서는 tossPaymentsAdapter나 다른 메서드가 호출되기 "전"에
        //  실패해야 하므로, 다른 given() 대본은 필요 없습니다.)


        // --- WHEN (실행) & THEN (결과) ---
        // 1. "confirmPayment()를 실행할 때 (금액이 다르므로),
        //    PaymentMismatchException이 발생하는 것을 기대한다."
        assertThatThrownBy(() -> {
            paymentService.confirmPayment(dto, userId);

            // 2. '예외 타입' 검증
        }).isInstanceOf(PaymentMismatchException.class)

                // 3. '예외 메시지' 검증
                .hasMessageContaining("결제 금액이 일치하지 않습니다");


        // 4. (가장 중요) '행위' 검증
        // [핵심] 검증(if) 단계에서 실패했으므로,
        //      API 호출이나 DB 저장은 '절대' 호출되면 안 됨.
        verify(tossPaymentsAdapter, never()).confirmPaymentToToss(any(PaymentConfirmRequest.class));
        verify(paymentProcessor, never()).processPaymentSuccessDB(any(Order.class), any(Map.class), anyLong());
        verify(paymentProcessor, never()).processPaymentFailDB(any(Order.class));
    }

    @Test
    @DisplayName("시나리오 B1: (환불) 완료된 주문 취소(cancelOrder)에 성공한다.")
    void cancelOrder_Success() {

        // --- GIVEN (주어진 것) ---
        // 1. 상수 정의
        Long userId = 1L;
        Long orderId = 1L;
        String paymentKey = "pk_test_real_payment_key";
        String cancelReason = "테스트 취소";

        // 2. DTO 생성
        OrderCancelRequest dto = new OrderCancelRequest(cancelReason);

        // 3. '가짜 엔티티' 생성
        User fakeUser = User.builder().id(userId).build();

        // (조회될 '가짜' 결제 완료 건)
        Payment fakePayment = Payment.builder()
                .pgTransactionId(paymentKey) // 👈 [핵심] 토스 취소 API에 전달할 키
                .build();

        // (조회될 '가짜' 주문)
        Order fakeOrder = Order.builder()
                .id(orderId)
                .user(fakeUser) // 👈 1L 유저 소유
                .deliveryStatus(DeliveryStatus.DELIVERED) // 👈 "CANCELLED"가 아님 (검증 통과)
                .build();

        // (조회될 '가짜' 토스 응답)
        Map<String, Object> tossCancelResponse = Map.of(
                "status", "CANCELED"
        );

        // 4. 'Mock' Repository 행동 정의 (Stubbing)

        doReturn(fakeOrder).when(orderRepository).getByIdWithDetailsOrThrow(orderId);

        // "paymentRepository.findByOrder(fakeOrder)가 호출되면, fakePayment를 반환해라"
        given(paymentRepository.findByOrder(fakeOrder)).willReturn(Optional.of(fakePayment));

        // [핵심] "tossPaymentsAdapter.cancelPaymentToToss가 호출되면, 가짜 취소 응답을 반환해라"
        given(tossPaymentsAdapter.cancelPaymentToToss(paymentKey, cancelReason))
                .willReturn(tossCancelResponse);

        // [핵심] "processPaymentCancelDB는 void 메서드이므로, 호출 시 아무것도 하지 않도록 설정"
        doNothing().when(paymentProcessor).processPaymentCancelDB(any(Order.class), any(Payment.class), any(Map.class));


        // --- WHEN (실행) ---
        // '진짜' paymentService의 cancelOrder 메서드를 호출
        paymentService.cancelOrder(orderId, userId, dto);


        // --- THEN (결과) ---
        // '행위(Behavior)' 검증

        // "orderRepository.getByIdWithDetailsOrThrow가 '정확히 1번' 호출되었는가?"
        verify(orderRepository, times(1)).getByIdWithDetailsOrThrow(orderId);

        // "paymentRepository.findByOrder가 '정확히 1번' 호출되었는가?"
        verify(paymentRepository, times(1)).findByOrder(fakeOrder);

        // "tossPaymentsAdapter.cancelPaymentToToss가 '정확히 1번' 호출되었는가?"
        verify(tossPaymentsAdapter, times(1)).cancelPaymentToToss(paymentKey, cancelReason);

        // [핵심] "API 취소가 성공했으니, processPaymentCancelDB가 '정확히 1번' 호출되었는가?"
        verify(paymentProcessor, times(1)).processPaymentCancelDB(fakeOrder, fakePayment, tossCancelResponse);
    }

    @Test
    @DisplayName("시나리오 B3: 타인의 주문을 취소하려고 하면 PaymentMismatchException이 발생한다.")
    void cancelOrder_Fail_AccessDenied() {

        // --- GIVEN (주어진 것) ---
        // 1. 상수 정의
        Long orderId = 1L;
        Long orderOwnerUserId = 100L;
        Long attackerUserId = 999L;

        // 2. DTO 생성 (내용은 이 테스트에서 중요하지 않음)
        OrderCancelRequest dto = new OrderCancelRequest("타인 주문 취소 시도");

        // 3. '가짜 엔티티' 생성
        User fakeOrderOwner = User.builder().id(orderOwnerUserId).build(); // 100번 유저

        Order fakeOrder = Order.builder()
                .id(orderId)
                .user(fakeOrderOwner) // 👈 주문은 100번 유저 소유
                .deliveryStatus(DeliveryStatus.DELIVERED) // (중복 취소 검증은 통과하도록)
                .build();

        // 4. 'Mock' Repository 행동 정의 (Stubbing)

        // "orderRepository.findByIdWithDetails(1L)가 호출되면,
        //  '100번 유저'가 주인인 fakeOrder를 정상 반환해라."
        doReturn(fakeOrder).when(orderRepository).getByIdWithDetailsOrThrow(orderId);


        // --- WHEN (실행) & THEN (결과) ---
        // [핵심] "999번 유저(공격자)가 1번 주문(100번 유저 소유) 취소를 시도할 때,
        //        PaymentMismatchException이 발생하는 것을 기대한다."
        assertThatThrownBy(() -> {
            // [WHEN] 999L (attackerUserId)로 메서드 호출
            paymentService.cancelOrder(orderId, attackerUserId, dto);

            // [THEN 1] '예외 타입' 검증
        }).isInstanceOf(PaymentMismatchException.class)

                // [THEN 2] (선택) '예외 메시지' 검증
                .hasMessageContaining("주문자 정보가 일치하지 않습니다.");


        // [THEN 3] (가장 중요) '행위' 검증
        // [핵심] 보안 검증(if) 단계에서 실패했으므로,
        //      API 호출이나 DB 저장은 '절대' 호출되면 안 됨.
        verify(paymentRepository, never()).findByOrder(any(Order.class));
        verify(tossPaymentsAdapter, never()).cancelPaymentToToss(anyString(), anyString());
        verify(paymentProcessor, never()).processPaymentCancelDB(any(Order.class), any(Payment.class), any(Map.class));
    }

    @Test
    @DisplayName("시나리오 B4: 이미 취소된 주문을 중복 취소하면 RuntimeException이 발생한다.")
    void cancelOrder_Fail_AlreadyCanceled() {

        // --- GIVEN (주어진 것) ---
        // 1. 상수 정의
        Long orderId = 1L;
        Long userId = 1L;

        // 2. DTO 생성 (내용은 중요하지 않음)
        OrderCancelRequest dto = new OrderCancelRequest("중복 취소 시도");

        // 3. '가짜 엔티티' 생성
        User fakeUser = User.builder().id(userId).build();

        // [핵심] 'deliveryStatus'가 이미 CANCELLED 상태인 '가짜 주문'
        Order fakeOrder = Order.builder()
                .id(orderId)
                .user(fakeUser) // 👈 1L 유저 소유 (사용자 검증은 통과)
                .deliveryStatus(DeliveryStatus.CANCELLED) // 👈 [핵심] 이미 취소된 상태
                .build();

        // 4. 'Mock' Repository 행동 정의 (Stubbing)

        // "orderRepository.findByIdWithDetails(1L)가 호출되면,
        //  '이미 취소된' fakeOrder를 정상 반환해라."
//        given(orderRepository.findByIdWithDetails(orderId)).willReturn(Optional.of(fakeOrder));
        doReturn(fakeOrder).when(orderRepository).getByIdWithDetailsOrThrow(orderId);

        // (이 테스트는 paymentRepository, tossPaymentsAdapter 등이 호출되기 "전"에
        //  실패해야 하므로, 다른 GIVEN 대본은 필요 없습니다.)


        // --- WHEN (실행) & THEN (결과) ---
        // [핵심] "이미 취소된 주문(orderId=1L) 취소를 시도할 때,
        //        RuntimeException이 발생하는 것을 기대한다."
        assertThatThrownBy(() -> {
            // [WHEN] 메서드 호출
            paymentService.cancelOrder(orderId, userId, dto);

            // [THEN 1] '예외 타입' 검증
        }).isInstanceOf(RuntimeException.class) // (커스텀 예외가 있다면 그것으로 변경)

                // [THEN 2] (선택) '예외 메시지' 검증
                .hasMessageContaining("이미 취소된 주문입니다");


        // [THEN 3] (가장 중요) '행위' 검증
        // [핵심] 상태 검증(if) 단계에서 실패했으므로,
        //      API 호출이나 DB 저장은 '절대' 호출되면 안 됨.
        verify(paymentRepository, never()).findByOrder(any(Order.class));
        verify(tossPaymentsAdapter, never()).cancelPaymentToToss(anyString(), anyString());
        verify(paymentProcessor, never()).processPaymentCancelDB(any(Order.class), any(Payment.class), any(Map.class));
    }

    @Test
    @DisplayName("시나리오 B5: Toss API 환불(취소) 실패 시, DB 작업을 롤백하고 예외를 던진다.")
    void cancelOrder_Fail_TossApiError() {

        // --- GIVEN (주어진 것) ---
        // 1. 상수 정의
        Long orderId = 1L;
        Long userId = 1L;
        String paymentKey = "pk_test_real_payment_key";
        String cancelReason = "API 실패 테스트";

        // 2. DTO 생성
        OrderCancelRequest dto = new OrderCancelRequest(cancelReason);

        // 3. '가짜 엔티티' 생성 (모두 정상)
        User fakeUser = User.builder().id(userId).build();
        Payment fakePayment = Payment.builder()
                .pgTransactionId(paymentKey)
                .build();
        Order fakeOrder = Order.builder()
                .id(orderId)
                .user(fakeUser)
                .deliveryStatus(DeliveryStatus.DELIVERED) // 👈 상태 검증 통과
                .build();

        // 4. 'Mock' Repository 행동 정의 (Stubbing)

        // (정상) 주문 및 결제 정보 조회 성공
//        given(orderRepository.findByIdWithDetails(orderId)).willReturn(Optional.of(fakeOrder));
        doReturn(fakeOrder).when(orderRepository).getByIdWithDetailsOrThrow(orderId);

        given(paymentRepository.findByOrder(fakeOrder)).willReturn(Optional.of(fakePayment));

        // [핵심] "tossPaymentsAdapter.cancelPaymentToToss가 호출되면,
        //      RuntimeException을 강제로 발생(throw)시켜라."
        given(tossPaymentsAdapter.cancelPaymentToToss(paymentKey, cancelReason))
                .willThrow(new RuntimeException("Toss API 환불 실패"));

        // (processPaymentCancelDB는 호출되지 않아야 하므로 GIVEN 대본이 필요 없음)


        // --- WHEN (실행) & THEN (결과) ---
        // 1. "cancelOrder()를 실행할 때 (Toss API가 실패하므로),
        //    RuntimeException이 발생하는 것을 기대한다."
        assertThatThrownBy(() -> {
            // [WHEN] 메서드 호출
            paymentService.cancelOrder(orderId, userId, dto);

            // [THEN 1] '예외 타입' 검증
        }).isInstanceOf(RuntimeException.class)

                // [THEN 2] '예외 메시지' 검증
          .hasMessageContaining("결제 취소 API 호출에 실패했습니다"); // 👈 PaymentService가 감싸서 던진 예외

        // [THEN 3] (가장 중요) '행위' 검증

        // "tossPaymentsAdapter는 '정확히 1번' 호출되었는가?" (호출 시도 자체는 했음)
        verify(tossPaymentsAdapter, times(1)).cancelPaymentToToss(paymentKey, cancelReason);

        // [핵심] "API가 실패했으니, DB 롤백(재고 복구) 로직은 '절대' 호출되면 안 됨."
        verify(paymentProcessor, never()).processPaymentCancelDB(any(Order.class), any(Payment.class), any(Map.class));
    }

    @Test
    @DisplayName("시나리오: DB에 주문이 없어도 Redis 캐시에서 조회하여 결제 승인을 진행한다.")
    void confirmPayment_Success_FromRedisCache() {
        // --- GIVEN ---
        Long userId = 1L;
        String orderNumber = "ORD-REDIS-TEST";
        Long amount = 20000L;
        PaymentConfirmRequest dto = new PaymentConfirmRequest("pk_test", orderNumber, amount);

        // 1. DB 조회 실패 설정 (null 반환)
        // (Service 코드가 .orElse(null)을 쓰므로 Optional.empty()를 리턴하면 null이 됨)
        given(orderRepository.findByOrderNumberWithItems(orderNumber)).willReturn(Optional.empty());

        // 2. Redis 캐시 조회 성공 설정
        // (PendingOrder DTO 생성 -> Entity 변환 로직 검증)
        PendingOrder cachedOrder = new PendingOrder(
                userId, orderNumber, amount, List.of(),
                "테스터", "010-1234-5678", "서울", "문앞"
        );
        given(redisStockService.getCachedOrder(orderNumber)).willReturn(cachedOrder);

        // 3. Toss Adapter 성공 설정
        Map<String, Object> tossResponse = Map.of("status", "DONE", "paymentKey", "pk_test");
        given(tossPaymentsAdapter.confirmPaymentToToss(dto)).willReturn(tossResponse);

        // 4. Processor 호출 설정
        doNothing().when(paymentProcessor).processPaymentSuccessDB(any(Order.class), any(Map.class), eq(userId));


        // --- WHEN ---
        PaymentConfirmResponse response = paymentService.confirmPayment(dto, userId);


        // --- THEN ---
        assertThat(response).isNotNull();

        // 1. DB 조회는 했으나 실패했음
        verify(orderRepository, times(1)).findByOrderNumberWithItems(orderNumber);

        // 2. [핵심] Redis 조회를 수행했음
        verify(redisStockService, times(1)).getCachedOrder(orderNumber);

        // 3. 정상적으로 결제 승인까지 이어짐
        verify(paymentProcessor, times(1)).processPaymentSuccessDB(any(Order.class), eq(tossResponse), eq(userId));
    }

}
