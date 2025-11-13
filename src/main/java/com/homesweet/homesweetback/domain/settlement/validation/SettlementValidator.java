package com.homesweet.homesweetback.domain.settlement.validation;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
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
        // 신규 주문건
        if (unsettledOrders.isEmpty()) {
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND);
        }
    }

    // 정산 가능한 주문인지 확인
    public void validateOrder(Order order) {
        validateExist(order);
        validateOrderCanceled(order);
        validateDeliveryStatus(order);
        validateOrderStatus(order);
        validateOrderItems(order);
        validateDuplicateOrder(order);
    }

    // 1. 주문이 있는지
    private void validateExist(Order order) {
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDERS_NOT_FOUND);
        }
    }

    // 2. 주문 상태가 주문 완료인지 확인
    private void validateOrderStatus(Order order) {
        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
    }
    // 6. 주문 취소된 건이 아닌지 확인 -> 주문 취소된 건은 정산 불가
    private void validateOrderCanceled(Order order) {
        if (order.getDeliveryStatus() == DeliveryStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ORDER_CANCELED_NOT_FOUND);
        }
    }

    // 3. 배송 상태가 배송 완료인지
    private void validateDeliveryStatus(Order order) {
        if (order.getDeliveryStatus() != DeliveryStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.DELIVERY_STATUS_NOT_DELIVERED);
        }
    }

    // 4. 주문 제품이 비어있는 지 확인
    private void validateOrderItems(Order order) {
        if (order.isOrderItemEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_ITEMS_EMPTY);
        }
    }

    // 5. 기존 주문 건이 아닌지 확인 -> 신규 정산건만 가능 -> 중복 확인
    private void validateDuplicateOrder(Order order) {
        if (settlementRepository.findByOrderId(order.getId()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_SETTLEMENT);
        }
    }

    // UserRole.SELLER인지 확인
    public void validateSeller(User seller) {
        validateSellerExist(seller);
        validateSellerRole(seller);
    }

    // 1. 판매자가 있는지 확인
    private void validateSellerExist(User seller) {
        if (seller == null) {
            throw new BusinessException(ErrorCode.SELLER_NOT_FOUND);
        }
    }

    private void validateSellerRole(User seller) {
        // 2. 역할이 판매자인지 확인
        if (seller.getRole() != UserRole.SELLER) {
            throw new BusinessException(ErrorCode.INVALID_SELLER_ROLE);
        }
    }

    // 주문 취소시 환불 금액 반영 및 정산 금액 변경
    public void validateCanceled(Settlement settlement, Order order) {
        validateSettlementAlreadyCanceled(settlement);
        validateDeliveryStatusCanceled(order);
    }

    // 1. 이미 취소된 정산인지
    private void validateSettlementAlreadyCanceled(Settlement settlement){
        if("CANCELED".equalsIgnoreCase(settlement.getSettlementStatus())){
            throw new BusinessException(ErrorCode.ALREADY_SETTLEMENT_CANCELED);
        }
    }
    // 2. 배송상태가 취소인지
    private void validateDeliveryStatusCanceled(Order order) {
        if (order.getDeliveryStatus() != DeliveryStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
    }
}