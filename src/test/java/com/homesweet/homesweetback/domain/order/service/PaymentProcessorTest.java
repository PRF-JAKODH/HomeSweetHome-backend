package com.homesweet.homesweetback.domain.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.homesweetback.domain.order.dto.internal.PendingPayment;
import com.homesweet.homesweetback.domain.order.entity.*;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.order.repository.PaymentRepository;
import com.homesweet.homesweetback.domain.product.cart.repository.CartRepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.SkuJPARepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.SkuEntity;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorTest {

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

    // 👇 [추가] Redis 서비스 Mock
    @Mock
    private RedisStockService redisStockService;

    @InjectMocks
    private PaymentProcessor paymentProcessor;

    @Test
    @DisplayName("시나리오 4: processPaymentSuccessDB가 DB 저장 대신 Redis에 결제 정보를 Push한다.")
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
                .orderNumber("ORD-TEST-123") // OrderNumber 필수
                .user(fakeUser)
                .orderStatus(OrderStatus.PENDING)
                .totalAmount(20000L)
                .deliveryStatus(DeliveryStatus.BEFORE_SHIPMENT)
                .orderedAt(LocalDateTime.now())
                .build();

        List<OrderItem> fakeItemsList = List.of(fakeItem);
        Order spiedFakeOrder = Mockito.spy(fakeOrder);
        given(spiedFakeOrder.getOrderItems()).willReturn(fakeItemsList);

        // [Stubbing] Redis Push 동작 (void)
        doNothing().when(redisStockService).pushPendingPayment(any(PendingPayment.class));

        // Cart 삭제
        doNothing().when(cartRepository).deleteByUserIdAndSkuIdIn(anyLong(), anyList());

        // --- WHEN ---
        paymentProcessor.processPaymentSuccessDB(spiedFakeOrder, tossResponse, userId);

        // --- THEN ---

        // 1. [핵심] Redis에 결제 정보가 Push 되었는지 검증
        verify(redisStockService, times(1)).pushPendingPayment(any(PendingPayment.class));

        // 2. [중요] DB 저장은 절대 호출되지 않아야 함 (스케줄러가 할 일)
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class)); // 상태 변경도 스케줄러가 함

        // 3. Cart 삭제는 정상적으로 호출되어야 함
        verify(cartRepository, times(1)).deleteByUserIdAndSkuIdIn(eq(userId), eq(List.of(skuId)));
    }

    @Test
    @DisplayName("시나리오 B2: processPaymentCancelDB가 재고 복구, Order/Payment 상태 변경을 수행한다.")
    void processPaymentCancelDB_Success() {
        // (취소 로직은 기존 DB 롤백 방식을 유지한다고 가정하므로 변경 없음)
        // 단, Lock 메서드 이름 변경(findById -> findById) 주의

        // --- GIVEN ---
        Long skuId = 100L;
        Long quantity = 2L;
        Map<String, Object> tossResponse = Map.of("status", "CANCELED");

        SkuEntity fakeSku = SkuEntity.builder()
                .id(skuId)
                .product(ProductEntity.builder().build())
                .stockQuantity(10L)
                .build();
        OrderItem fakeItem = OrderItem.builder().sku(fakeSku).quantity(quantity).build();

        Order fakeOrder = Order.builder()
                .id(1L)
                .orderStatus(OrderStatus.COMPLETED)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .build();
        Payment fakePayment = Payment.builder().paymentStatus("DONE").build();

        List<OrderItem> fakeItemsList = List.of(fakeItem);
        Order spiedFakeOrder = Mockito.spy(fakeOrder);
        given(spiedFakeOrder.getOrderItems()).willReturn(fakeItemsList);

        // [수정] 락을 사용하는 조회 메서드로 Stubbing
        given(skuJPARepository.findById(skuId)).willReturn(Optional.of(fakeSku));

        // --- WHEN ---
        paymentProcessor.processPaymentCancelDB(spiedFakeOrder, fakePayment, tossResponse);

        // --- THEN ---
        // 1. 재고 복구 로직 호출 확인 (락 조회)
        verify(skuJPARepository, times(1)).findById(skuId);

        // 2. Order 상태 변경 확인
        assertThat(spiedFakeOrder.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(spiedFakeOrder.getDeliveryStatus()).isEqualTo(DeliveryStatus.CANCELLED);

        // 3. Payment 상태 변경 확인
        assertThat(fakePayment.getPaymentStatus()).isEqualTo("CANCELED");

        // 4. 변경 사항 저장 확인
        verify(orderRepository, times(1)).save(spiedFakeOrder);
    }
}