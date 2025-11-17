package com.homesweet.homesweetback.domain.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.homesweetback.domain.order.entity.*;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.order.repository.PaymentRepository;
import com.homesweet.homesweetback.domain.product.cart.repository.CartRepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.SkuJPARepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import com.homesweet.homesweetback.domain.auth.entity.User;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorTest {

    // [핵심] PaymentProcessor가 사용하는 의존성들만 @Mock으로 선언
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SkuJPARepository skuJPARepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private ObjectMapper objectMapper;

    // [핵심] 테스트 대상: PaymentProcessor
    @InjectMocks
    private PaymentProcessor paymentProcessor;

    @Test
    @DisplayName("시나리오 4: processPaymentSuccessDB가 Payment 저장, Order 상태변경, Cart 삭제를 수행한다.")
    void processPaymentSuccessDB_Success() {
        // --- GIVEN ---
        Long userId = 1L;
        Long skuId = 100L;
        Long quantity = 2L;

        // 가짜 응답
        Map<String, Object> tossResponse = Map.of(
                "paymentKey", "pk_test",
                "method", "CARD",
                "status", "DONE",
                "paidAt", "2025-11-17T10:00:00"
        );

        // 가짜 엔티티 생성
        User fakeUser = User.builder().id(userId).build();
        SkuEntity fakeSku = SkuEntity.builder().id(skuId).product(ProductEntity.builder().build()).build();
        OrderItem fakeItem = OrderItem.builder().sku(fakeSku).quantity(quantity).build();

        Order fakeOrder = Order.builder()
                .id(1L)
                .user(fakeUser)
                .orderStatus(OrderStatus.PENDING)
                .deliveryStatus(DeliveryStatus.BEFORE_SHIPMENT)
                .orderedAt(LocalDateTime.now())
                .build();

        // [핵심] 리스트 Stubbing (Spy 사용) - for문 실행을 위해 필수
        List<OrderItem> fakeItemsList = List.of(fakeItem);
        Order spiedFakeOrder = Mockito.spy(fakeOrder);
        given(spiedFakeOrder.getOrderItems()).willReturn(fakeItemsList);

        // Repository Stubbing
        // 1. Payment 저장
        given(paymentRepository.save(any(Payment.class))).willAnswer(i -> i.getArgument(0));

        // 2. Cart 삭제 (void)
        doNothing().when(cartRepository).deleteByUserIdAndSkuIdIn(anyLong(), anyList());

        // (참고: 재고 차감 로직은 삭제되었으므로 skuRepository Stubbing 필요 없음)

        // --- WHEN ---
        paymentProcessor.processPaymentSuccessDB(spiedFakeOrder, tossResponse, userId);

        // --- THEN ---
        // 1. Payment 저장 호출 확인
        verify(paymentRepository, times(1)).save(any(Payment.class));

        // 2. Cart 삭제 호출 확인
        verify(cartRepository, times(1)).deleteByUserIdAndSkuIdIn(eq(userId), eq(List.of(skuId)));

        // 3. Order 상태 변경 확인
        assertThat(spiedFakeOrder.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(spiedFakeOrder.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
    }

    @Test
    @DisplayName("시나리오 B2: processPaymentCancelDB가 재고 복구, Order/Payment 상태 변경을 수행한다.")
    void processPaymentCancelDB_Success() {
        // --- GIVEN ---
        Long skuId = 100L;
        Long quantity = 2L;
        Map<String, Object> tossResponse = Map.of("status", "CANCELED");

        // 가짜 엔티티 (재고 복구를 위해 stockQuantity 필수)
        SkuEntity fakeSku = SkuEntity.builder()
                .id(skuId)
                .product(ProductEntity.builder().build())
                .stockQuantity(10L) // 👈 초기 재고
                .build();
        OrderItem fakeItem = OrderItem.builder().sku(fakeSku).quantity(quantity).build();

        Order fakeOrder = Order.builder()
                .id(1L)
                .orderStatus(OrderStatus.COMPLETED)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .build();
        Payment fakePayment = Payment.builder().paymentStatus("DONE").build();

        // 리스트 Stubbing
        List<OrderItem> fakeItemsList = List.of(fakeItem);
        Order spiedFakeOrder = Mockito.spy(fakeOrder);
        given(spiedFakeOrder.getOrderItems()).willReturn(fakeItemsList);

        // Repository Stubbing (재고 복구용)
        given(skuJPARepository.findByIdWithPessimisticLock(skuId)).willReturn(Optional.of(fakeSku));

        // --- WHEN ---
        paymentProcessor.processPaymentCancelDB(spiedFakeOrder, fakePayment, tossResponse);

        // --- THEN ---
        // 1. 재고 복구 로직 호출 확인
        verify(skuJPARepository, times(1)).findByIdWithPessimisticLock(skuId);

        // 2. Order 상태 변경 확인
        assertThat(spiedFakeOrder.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(spiedFakeOrder.getDeliveryStatus()).isEqualTo(DeliveryStatus.CANCELLED);

        // 3. Payment 상태 변경 확인
        assertThat(fakePayment.getPaymentStatus()).isEqualTo("CANCELED");
    }
}