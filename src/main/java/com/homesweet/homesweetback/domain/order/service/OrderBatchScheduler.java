package com.homesweet.homesweetback.domain.order.service;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.order.dto.internal.PendingOrder;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderItem;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.SkuJPARepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.SkuEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class


OrderBatchScheduler {

    private final RedisStockService redisStockService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final SkuJPARepository skuJPARepository;

    // 1초마다 최대 1000개씩 처리
    @Scheduled(fixedRate = 1000)
    @Transactional
    public void processPendingOrders() {
        // 1. Redis에서 주문 꺼내기
        List<PendingOrder> pendingOrders = redisStockService.popPendingOrders(1000);
        if (pendingOrders.isEmpty()) return;

        log.info("[Order Batch] Redis에서 주문 {}건 처리 시작", pendingOrders.size());

        List<Order> ordersToSave = new ArrayList<>();

        // 2. 엔티티 변환
        for (PendingOrder dto : pendingOrders) {
            try {
                // (성능을 위해 getReferenceById 사용 권장하지만, 안전하게 findById 사용)
                User user = userRepository.findById(dto.userId()).orElseThrow();

                Order order = Order.builder()
                        .user(user)
                        .orderNumber(dto.orderNumber())
                        .totalAmount(dto.totalAmount())
                        .orderStatus(OrderStatus.PENDING)
                        .deliveryStatus(DeliveryStatus.BEFORE_SHIPMENT)
                        .orderedAt(LocalDateTime.now())
                        .build();

                for (PendingOrder.PendingOrderItem itemDto : dto.items()) {
                    SkuEntity sku = skuJPARepository.findById(itemDto.skuId()).orElseThrow();
                    OrderItem orderItem = OrderItem.builder()
                            .sku(sku)
                            .quantity((long) itemDto.quantity())
                            .price(itemDto.price())
                            .build();
                    order.addOrderItem(orderItem);
                }
                ordersToSave.add(order);
            } catch (Exception e) {
                log.error("주문 변환 실패. DLQ 이동: {}", dto.orderNumber());
                redisStockService.sendToOrderDLQ(dto);
            }
        }

        // 3. Bulk Insert (DB 저장)
//        orderRepository.saveAll(ordersToSave);
//        log.info("[Order Batch] DB 저장 완료: {}건", ordersToSave.size());

        if (ordersToSave.isEmpty()) return;

        try {
            orderRepository.saveAll(ordersToSave);
            log.info("[Order Batch] DB 저장 완료: {}건", ordersToSave.size());
        } catch (Exception e) {
            log.warn("[Order Batch] 일괄 저장 실패. 개별 저장으로 전환합니다. Error: {}", e.getMessage());

            // [2차 시도] 하나씩 저장하며 문제 있는 주문 격리 (Fallback)
            int successCount = 0;
            int failCount = 0;

            for (Order order : ordersToSave) {
                try {
                    // 개별 저장 시도
                    orderRepository.save(order);
                    successCount++;
                } catch (Exception individualEx) {
                    // [3차 조치] 저장 실패한 주문만 DLQ로 이동
                    failCount++;
                    log.error("주문 저장 최종 실패 (OrderNum: {}). DLQ 이동.", order.getOrderNumber());

                    // Order 엔티티를 다시 DTO로 변환해서 Redis DLQ에 넣음
                    sendToDlq(order);
                }
            }
            log.info("[Order Batch] 개별 처리 완료. 성공: {}, 실패(DLQ): {}", successCount, failCount);
        }
    }
    // --- 헬퍼 메서드: 엔티티 -> DTO 역변환 및 DLQ 전송 ---
    private void sendToDlq(Order order) {
        try {
            // Order 엔티티에서 필요한 정보를 뽑아 PendingOrder DTO를 다시 만듭니다.
            // (Item 정보 등은 엔티티에서 역추적)
            List<PendingOrder.PendingOrderItem> items = order.getOrderItems().stream()
                    .map(item -> new PendingOrder.PendingOrderItem(
                            item.getSku().getId(),
                            item.getQuantity().intValue(),
                            item.getPrice()
                    )).collect(Collectors.toList());

            PendingOrder failedOrderDto = new PendingOrder(
                    order.getUser().getId(),
                    order.getOrderNumber(),
                    order.getTotalAmount(),
                    items,
                    "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN"
                    // ⚠️ 주의: 엔티티에는 배송 정보(이름, 주소 등)가 저장되어 있지 않을 수 있음.
                    // 만약 Order 엔티티에 배송 정보 필드가 없다면,
                    // 1. 애초에 Redis에서 꺼낸 원본 DTO를 Map<OrderNumber, DTO>로 들고 있거나,
                    // 2. Order 엔티티에 배송 정보 필드를 추가해야 완벽한 복구가 가능함.
            );

            // Redis DLQ 리스트에 넣기
            redisStockService.sendToOrderDLQ(failedOrderDto);

        } catch (Exception e) {
            log.error("DLQ 전송마저 실패 (OrderNum: {}): {}", order.getOrderNumber(), e.getMessage());
            // 최후의 수단: 파일 로그나 슬랙 알림 발송
        }
    }

    }
