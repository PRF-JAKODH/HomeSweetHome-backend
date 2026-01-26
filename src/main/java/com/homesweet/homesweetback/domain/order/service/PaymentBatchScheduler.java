package com.homesweet.homesweetback.domain.order.service;

import com.homesweet.homesweetback.domain.order.dto.internal.PendingPayment;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Payment;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.order.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentBatchScheduler {

    private final RedisStockService redisStockService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Scheduled(fixedRate = 1000)
    public void processPendingPayments() {
        List<PendingPayment> payments = redisStockService.popPendingPayments(1000);
        if (payments.isEmpty()) return;

        log.info("[Payment Batch] 결제 정보 {}건 처리 시작", payments.size());

        Set<String> orderNumbers = payments.stream()
                .map(PendingPayment::orderNumber)
                .collect(Collectors.toSet());

        Map<String, Order> orderMap = orderRepository.findAllByOrderNumberIn(orderNumbers)
                .stream()
                .collect(Collectors.toMap(Order::getOrderNumber, Function.identity()));

        List<Payment> paymentsToSave = new ArrayList<>();
        List<Order> ordersToUpdate = new ArrayList<>();

        for (PendingPayment dto : payments) {
            try {
                Order order = orderMap.get(dto.orderNumber());

                if (order == null) {
                    // 재큐잉
                    log.warn("주문 정보 없음. 재큐잉 (OrderNum: {})", dto.orderNumber());
                    redisStockService.requeuePayment(dto);
                    continue;
                }

                // 2. Payment 엔티티 생성 및 저장
                Payment payment = Payment.builder()
                        .order(order)
                        .pgTransactionId(dto.pgTransactionId())
                        .amount(dto.amount())
                        .method(dto.method())
                        .paymentStatus(dto.paymentStatus())
                        .paidAt(dto.paidAt())
                        .pgRawData(dto.pgRawData())
                        .build();

                paymentsToSave.add(payment);

                // 3. Order 상태 업데이트
                order.setOrderStatus(OrderStatus.COMPLETED);
                order.setDeliveryStatus(DeliveryStatus.DELIVERED);
                ordersToUpdate.add(order);

            } catch (Exception e) {
                log.error("결제 저장 실패 (OrderNum: {}). DLQ로 이동.", dto.orderNumber());
                // 4. [DLQ] 진짜 에러가 난 경우 실패 보관함으로 이동
                redisStockService.sendToPaymentDLQ(dto);
            }
        }
        if (!paymentsToSave.isEmpty()) {
            savePaymentAndOrderSafe(paymentsToSave, ordersToUpdate);
        }
    }

    /**
     * DB 저장 로직 분리 (트랜잭션 적용)
     */
    @Transactional
    public void savePaymentAndOrderSafe(List<Payment> payments, List<Order> orders) {
        try {
            paymentRepository.saveAll(payments);
            orderRepository.saveAll(orders);
            log.info("[Payment Batch] DB 동기화 완료: Payment {}건, Order 완료 처리 {}건", payments.size(), orders.size());
        } catch (Exception e) {
            log.error("[Payment Batch] 일괄 저장 실패! (치명적 오류 - 수동 확인 필요): {}", e.getMessage());
            // 고급: 여기서 실패 시 Retry 로직 추가 가능
        }
    }
}

