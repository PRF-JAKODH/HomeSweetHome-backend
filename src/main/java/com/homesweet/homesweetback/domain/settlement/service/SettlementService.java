package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.grade.service.GradeService;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SettlementService {
    private final SettlementRepository settlementRepository;
    private final GradeService gradeService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    //TODO: 결제에서 받지말고 한번에 처리하게끔 구조를 변경
    // 주문 확정(결제 완료)시 정산 생성
    @Transactional
    public void createSettlement(Order order) {
        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("결제완료 상태인 경우에만 정산을 생성할 수 있어요");
        }
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException("제품이 없어요");
        }
        User seller = orderRepository.findBySellerId(order.getId());
        if (seller == null) {
            throw new IllegalArgumentException("판매자를 찾을 수 없어요");
        }
        if (seller.getRole() != UserRole.SELLER) {
            throw new IllegalArgumentException("유효한 판매자가 아닙니다");
        }
        BigDecimal fee = gradeService.calculateFeeforUser(BigDecimal.valueOf(order.getTotalAmount()), seller);
        //TODO: 계산 로직만 메서드를 분리한다면 순숫하게 테스트 가능
        BigDecimal refundAmount = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.valueOf(order.getTotalAmount()).multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalAmount = BigDecimal.valueOf(order.getTotalAmount());
        BigDecimal settlementAmount = totalAmount.subtract(fee).subtract(refundAmount).setScale(2, RoundingMode.HALF_UP);

        Settlement settlement = Settlement.builder()
                .order(order)
                .salesAmount(totalAmount)
                .fee(fee)
                .vat(vat)
                .refundAmount(refundAmount)
                .settlementAmount(settlementAmount)
                .settlementDate(LocalDateTime.now())
                .settlementStatus("PENDING")
                .userId(seller.getId())
                .build();

        settlementRepository.save(settlement);
    }
}