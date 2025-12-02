package com.homesweet.homesweetback.domain.order.service;

import com.homesweet.homesweetback.domain.order.dto.internal.PendingPayment;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Payment;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.order.repository.PaymentRepository;
import com.homesweet.homesweetback.domain.order.service.RedisStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentBatchScheduler {

    private final RedisStockService redisStockService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Scheduled(fixedRate = 1000)
    @Transactional
    public void processPendingPayments() {
        List<PendingPayment> payments = redisStockService.popPendingPayments(1000);
        if (payments.isEmpty()) return;

        log.info("[Payment Batch] 결제 정보 {}건 처리 시작", payments.size());

        for (PendingPayment dto : payments) {
            try {
                // 1. 주문 찾기 (OrderNumber로 조회)
                Optional<Order> orderOpt = orderRepository.findByOrderNumber(dto.orderNumber());

                if (orderOpt.isEmpty()) {
                    // 🚨 중요: 주문 스케줄러가 아직 안 돌아서 DB에 주문이 없는 경우
                    // -> 다시 Redis 대기열로 돌려보냄 (Retry)
                    log.warn("주문 정보 없음. 재큐잉 (OrderNum: {})", dto.orderNumber());
                    redisStockService.requeuePayment(dto);
                    continue;
                }

                Order order = orderOpt.get();

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

                paymentRepository.save(payment);

                // 3. Order 상태 업데이트
                order.setOrderStatus(OrderStatus.COMPLETED);
                order.setDeliveryStatus(DeliveryStatus.DELIVERED);
                orderRepository.save(order);

            } catch (Exception e) {
                log.error("결제 저장 실패 (OrderNum: {}). DLQ로 이동.", dto.orderNumber());
                // 4. [DLQ] 진짜 에러가 난 경우 실패 보관함으로 이동
                redisStockService.sendToPaymentDLQ(dto);
            }
        }
    }
}