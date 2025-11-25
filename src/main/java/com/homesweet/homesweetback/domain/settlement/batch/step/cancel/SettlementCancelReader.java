package com.homesweet.homesweetback.domain.settlement.batch.step.cancel;

import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@StepScope
@RequiredArgsConstructor
public class SettlementCancelReader implements ItemReader<Order> {
    private final SettlementRepository settlementRepository;

    @Value("#{jobParameters['cutoff']}")
    private String cutoffString;

    private ListItemReader<Order> orderListItemReader;

    public Order read() {
        // 1. step 시작시 최초 1회만 DB 조회를 위해 초기화 ->  DB 호출 이후에는 조회 X -> 신규 정산 건이 있으면
        if (orderListItemReader == null) {
            // LocalDateTime 직접 주입 불가
            LocalDateTime cutoff = LocalDateTime.parse(cutoffString);
            // 주문 취소건 조회
            List<Order> cancelOrders = settlementRepository.findCancelSettlement(DeliveryStatus.CANCELLED, cutoff);
            // 조회 결과를 reader에게 위임 -> 이후 order 하나씩 반환
            orderListItemReader = new ListItemReader<>(cancelOrders);
        }
        // 2. 초기화 이후 Order을 1개씩 반환
        return orderListItemReader.read();
    }
}
