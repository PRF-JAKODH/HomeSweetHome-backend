package com.homesweet.homesweetback.domain.order.service;

import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderItem;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.SkuJPARepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderSchedulerService {

    private final OrderRepository orderRepository;
    private final SkuJPARepository skuJPARepository;
    private final RedisStockService redisStockService;

    // 60분으로 설정
    private static final int ABANDONED_ORDER_TIMEOUT_MINUTES = 60;

    /**
     * 매 30분마다 실행 (예: 1:00, 1:30, 2:00 ...)
     * * 생성된 지 60분이 지났지만 여전히 PENDING 상태인
     * '결제 이탈 주문'을 찾아 자동으로 취소 처리합니다.
     */
    @Scheduled(cron = "0 0/30 * * * ?") // 30분마다 실행
    public void cleanupAbandonedPendingOrders() {
        log.info("[Scheduler] 결제 이탈 주문(PENDING) 자동 취소 작업 시작...");

        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(ABANDONED_ORDER_TIMEOUT_MINUTES);

        List<Order> ordersToCancel = orderRepository
                .findAllByOrderStatusAndOrderedAtBefore(OrderStatus.PENDING, cutoffTime);

        if (ordersToCancel.isEmpty()) {
            log.info("[Scheduler] 자동 취소할 주문이 없습니다.");
            return;
        }

        log.info("[Scheduler] 총 {}건의 결제 이탈 주문을 자동 취소합니다.", ordersToCancel.size());


        // 상태 변경 및 재고 복구
        for (Order order : ordersToCancel) {
                try {
                    this.cancelSingleOrder(order);
                } catch (Exception e) {
                    log.error("[Scheduler Error] 주문(ID:{}) 취소 처리 중 오류 발생: {}", order.getId(), e.getMessage());
                }
            }
        log.info("[Scheduler] 자동 취소 작업 완료.");
    }

    // [신규] 개별 주문 취소 로직 (트랜잭션 분리 효과)
    @Transactional // 주문 1건 단위로 트랜잭션 보장
    public void cancelSingleOrder(Order order) {

        // 1. 상태 변경
        order.setOrderStatus(OrderStatus.FAILED);
        order.setDeliveryStatus(DeliveryStatus.CANCELLED);

        // 2. 재고 복구 (Redis + DB)
        for (OrderItem item : order.getOrderItems()) {
            Long skuId = item.getSku().getId();
            Long quantity = (long) item.getQuantity();

            try {
                // [핵심 1] Redis 재고 복구 (가장 중요!)
                redisStockService.increaseStock(skuId, quantity);

                // [핵심 2] DB 재고 복구 (동기화)
                // (Lock 없이 단순 조회 후 업데이트 - 스케줄러라 경합 가능성 낮음)
                skuJPARepository.findById(skuId).ifPresent(sku -> {
                    sku.increaseStock(quantity);
                });

            } catch (Exception e) {
                // 특정 상품 재고 복구 실패해도 주문 상태 변경은 진행 (로그만 남김)
                log.error("[Scheduler] 재고 복구 실패 (OrderId: {}, SkuId: {}): {}",
                        order.getId(), skuId, e.getMessage());
            }
        }
        // 3. 변경 사항 저장
        // (@Transactional이 있어서 Dirty Checking으로 자동 저장되지만, 명시적으로 호출해도 됨)
        orderRepository.save(order);
    }
}