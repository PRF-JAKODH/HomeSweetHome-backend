package com.homesweet.homesweetback.domain.order.service;

import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderItem;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.SkuJPARepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.SkuEntity;
import jakarta.persistence.EntityNotFoundException;
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

    // 60분으로 설정
    private static final int ABANDONED_ORDER_TIMEOUT_MINUTES = 60;

    /**
     * 매 30분마다 실행 (예: 1:00, 1:30, 2:00 ...)
     * * 생성된 지 60분이 지났지만 여전히 PENDING 상태인
     * '결제 이탈 주문'을 찾아 자동으로 취소 처리합니다.
     */
    @Scheduled(cron = "0 0/30 * * * ?") // 30분마다 실행
    @Transactional
    public void cleanupAbandonedPendingOrders() {
        log.info("[Scheduler] 결제 이탈 주문(PENDING) 자동 취소 작업 시작...");

        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(ABANDONED_ORDER_TIMEOUT_MINUTES);

        List<Order> ordersToCancel = orderRepository
                .findAllByOrderStatusAndOrderedAtBefore(OrderStatus.PENDING, cutoffTime);

        if (ordersToCancel.isEmpty()) {
            log.info("[Scheduler] 자동 취소할 주문이 없습니다.");
            return;
        }

        log.warn("[Scheduler] 총 {}건의 결제 이탈 주문을 자동 취소합니다.", ordersToCancel.size());


        // 상태 변경 및 재고 복구
        for (Order order : ordersToCancel) {

            // 주문 상태 변경 (FAILED, CANCELLED)
            order.setOrderStatus(OrderStatus.FAILED);
            order.setDeliveryStatus(DeliveryStatus.CANCELLED);

            // 재고 복구
            // createOrder에서 차감했던 재고를 다시 늘려줍니다.
            for (OrderItem item : order.getOrderItems()) {
                try {
                    // (동시성 제어를 위해 비관적 락 사용)
                    SkuEntity sku = skuJPARepository.findByIdWithPessimisticLock(item.getSku().getId())
                            .orElseThrow(() -> new EntityNotFoundException("스케줄러: SKU를 찾을 수 없습니다: " + item.getSku().getId()));

                    sku.increaseStock(item.getQuantity()); // (가정한 재고 증가 메서드)

                } catch (Exception e) {
                    // (중요) 재고 복구에 실패하더라도, 주문 상태 변경(CANCELLED)은 롤백되면 안 됩니다.
                    //      (주문은 취소됐는데 재고만 복구 안 된 상태 -> 별도 모니터링 필요)
                    log.error("[Scheduler CRITICAL] 주문(id:{}) 재고 복구 실패. SKU ID: {}, Error: {}",
                            order.getId(), item.getSku().getId(), e.getMessage());
                }
            }
        }
        log.info("[Scheduler] 자동 취소 작업 완료.");
    }
}