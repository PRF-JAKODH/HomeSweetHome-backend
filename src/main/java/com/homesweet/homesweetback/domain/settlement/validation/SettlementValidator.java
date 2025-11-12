package com.homesweet.homesweetback.domain.settlement.validation;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

// 정산 생성 검증 클래스
@Component
@RequiredArgsConstructor
public class SettlementValidator {
    private final SettlementRepository settlementRepository;

    public void validateUnsettledOrders(List<Order> unsettledOrders) {
        if (unsettledOrders.isEmpty()) {
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND);
        }
    }

    // 주문 확인
    public void validateOrder(Order order) {
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDERS_NOT_FOUND);
        }
        // 1. 주문 상태가 주문 완료인지 확인
        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_CREATED);
        }
        // 2. 배송 상태가 배송완료인지
        if(order.getDeliveryStatus() != DeliveryStatus.DELIVERED){
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_CREATED);
        }
        // 3. 주문 제품이 비어있는 지 확인
        if (order.isOrderItemEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_ITEMS_EMPTY);
        }
        // 4. 기존 주문 건이 아닌지 확인 -> 신규 정산건만 가능 -> 중복 확인
        if(settlementRepository.findByOrderId(order.getId()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_SETTLEMENT);
        }
        // 5. 주문 취소된 건이 아닌지 확인 -> 주문 취소된 건은 정산 불가
        if (order.getDeliveryStatus() == DeliveryStatus.CANCELLED){
            throw new BusinessException(ErrorCode.ORDER_CANCELED_NOT_FOUND);
        }
    }

    // UserRole.SELLER인지 확인
    public void validateSeller(User seller) {
        if (seller == null) {
            throw new BusinessException(ErrorCode.SELLER_NOT_FOUND);
        }
        if (seller.getRole() != UserRole.SELLER) {
            throw new BusinessException(ErrorCode.INVALID_SELLER_ROLE);
        }
    }
}
