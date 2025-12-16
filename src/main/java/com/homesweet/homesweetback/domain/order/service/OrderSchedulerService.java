// package com.homesweet.homesweetback.domain.order.service;

// import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
// import com.homesweet.homesweetback.domain.order.entity.Order;
// import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
// import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.time.LocalDateTime;
// import java.util.List;

// @Service
// @Slf4j
// @RequiredArgsConstructor
// public class OrderSchedulerService {

// private final OrderRepository orderRepository;

// // 60분으로 설정
// private static final int ABANDONED_ORDER_TIMEOUT_MINUTES = 60;

// /**
// * 매 30분마다 실행 (예: 1:00, 1:30, 2:00 ...)
// * * 생성된 지 60분이 지났지만 여전히 PENDING 상태인
// * '결제 이탈 주문'을 찾아 자동으로 취소 처리합니다.
// */
// @Scheduled(cron = "0 0/30 * * * ?") // 30분마다 실행
// @Transactional
// public void cleanupAbandonedPendingOrders() {
// log.info("[Scheduler] 결제 이탈 주문(PENDING) 자동 취소 작업 시작...");

// LocalDateTime cutoffTime =
// LocalDateTime.now().minusMinutes(ABANDONED_ORDER_TIMEOUT_MINUTES);

// List<Order> ordersToCancel = orderRepository
// .findAllByOrderStatusAndOrderedAtBefore(OrderStatus.PENDING, cutoffTime);

// if (ordersToCancel.isEmpty()) {
// log.info("[Scheduler] 자동 취소할 주문이 없습니다.");
// return;
// }

// log.warn("[Scheduler] 총 {}건의 결제 이탈 주문을 자동 취소합니다.", ordersToCancel.size());

// for (Order order : ordersToCancel) {
// order.setOrderStatus(OrderStatus.FAILED);
// order.setDeliveryStatus(DeliveryStatus.CANCELLED);
// }
// log.info("[Scheduler] 자동 취소 작업 완료.");
// }
// }