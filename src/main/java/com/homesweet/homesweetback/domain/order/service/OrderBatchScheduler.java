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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderBatchScheduler {

    private final RedisStockService redisStockService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final SkuJPARepository skuJPARepository;

    // 1초마다 최대 1000개씩 처리
    @Scheduled(fixedRate = 1000)
    // 주의: Redis Pop은 트랜잭션 밖에서 일어나는 게 좋지만,
    // 여기서는 DB 저장 일관성을 위해 묶되, 예외 처리를 꼼꼼히 함.
    public void processPendingOrders() {
        // 1. Redis에서 주문 꺼내기
        List<PendingOrder> pendingOrders = redisStockService.popPendingOrders(1000);
        if (pendingOrders.isEmpty()) return;

        log.info("[Order Batch] Redis에서 주문 {}건 처리 시작", pendingOrders.size());

        // [개선 1] 원본 DTO 보관용 맵 생성 (DLQ 전송 시 정보 손실 방지)
        Map<String, PendingOrder> originalDtoMap = pendingOrders.stream()
                .collect(Collectors.toMap(PendingOrder::orderNumber, Function.identity(), (p1, p2) -> p1));

        // [개선 2] Bulk 조회를 위한 ID 추출 (N+1 방지)
        Set<Long> userIds = pendingOrders.stream().map(PendingOrder::userId).collect(Collectors.toSet());
        Set<Long> skuIds = pendingOrders.stream()
                .flatMap(o -> o.items().stream().map(PendingOrder.PendingOrderItem::skuId))
                .collect(Collectors.toSet());

        // [개선 2] DB에서 한 번에 조회 (In-Query)
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, SkuEntity> skuMap = skuJPARepository.findAllById(skuIds).stream()
                .collect(Collectors.toMap(SkuEntity::getId, Function.identity()));


        List<Order> ordersToSave = new ArrayList<>();
        List<PendingOrder> failedOrders = new ArrayList<>(); // 변환 실패 목록

        // 3. 엔티티 변환
        for (PendingOrder dto : pendingOrders) {
            try {
                User user = userMap.get(dto.userId());
                if (user == null) throw new RuntimeException("User Not Found: " + dto.userId());

                Order order = Order.builder()
                        .user(user)
                        .orderNumber(dto.orderNumber())
                        .totalAmount(dto.totalAmount())
                        .orderStatus(OrderStatus.PENDING)
                        .deliveryStatus(DeliveryStatus.BEFORE_SHIPMENT)
                        .orderedAt(LocalDateTime.now())
                        // [주의] 배송 정보도 Entity에 저장해야 한다면 여기서 set 해야 함!
                        // .recipientName(dto.recipientName()) ...
                        .build();

                for (PendingOrder.PendingOrderItem itemDto : dto.items()) {
                    SkuEntity sku = skuMap.get(itemDto.skuId());
                    if (sku == null) throw new RuntimeException("SKU Not Found: " + itemDto.skuId());

                    OrderItem orderItem = OrderItem.builder()
                            .sku(sku)
                            .quantity((long) itemDto.quantity())
                            .price(itemDto.price())
                            .build();
                    order.addOrderItem(orderItem);
                }
                ordersToSave.add(order);

            } catch (Exception e) {
                log.error("주문 변환 실패 (OrderNum: {}): {}", dto.orderNumber(), e.getMessage());
                // 변환 단계에서 실패한 건 바로 DLQ
                redisStockService.sendToOrderDLQ(dto);
            }
        }

        if (ordersToSave.isEmpty()) return;

        // 4. DB 저장 (일괄 -> 실패 시 개별)
        saveOrdersSafely(ordersToSave, originalDtoMap);
    }

    // 트랜잭션 분리 및 안전 저장 로직
    private void saveOrdersSafely(List<Order> ordersToSave, Map<String, PendingOrder> originalDtoMap) {
        try {
            // [Happy Path] 일괄 저장 시도
            // 여기서 트랜잭션이 시작되고 커밋됨
            saveAllTransactional(ordersToSave);
            log.info("[Order Batch] DB 저장 완료: {}건", ordersToSave.size());

        } catch (Exception e) {
            log.warn("[Order Batch] 일괄 저장 실패. 개별 저장으로 전환. Error: {}", e.getMessage());

            // [Fallback] 하나씩 저장
            int successCount = 0;
            int failCount = 0;

            for (Order order : ordersToSave) {
                try {
                    saveOneTransactional(order);
                    successCount++;
                } catch (Exception individualEx) {
                    failCount++;
                    log.error("주문 개별 저장 실패 (OrderNum: {}). DLQ 이동.", order.getOrderNumber());

                    // [핵심] 원본 DTO를 찾아서 DLQ로 보냄 (정보 손실 없음)
                    PendingOrder originalDto = originalDtoMap.get(order.getOrderNumber());
                    if (originalDto != null) {
                        redisStockService.sendToOrderDLQ(originalDto);
                    } else {
                        log.error("DLQ 전송 실패: 원본 DTO 유실 (OrderNum: {})", order.getOrderNumber());
                    }
                }
            }
            log.info("[Order Batch] 개별 처리 완료. 성공: {}, 실패(DLQ): {}", successCount, failCount);
        }
    }

    @Transactional
    public void saveAllTransactional(List<Order> orders) {
        orderRepository.saveAll(orders);
    }

    @Transactional
    public void saveOneTransactional(Order order) {
        orderRepository.save(order);
    }
}