package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.grade.service.GradeService;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SettlementService {
    private final SettlementRepository settlementRepository;
    private final GradeService gradeService;
    private final UserRepository userRepository;
    // 주문 확정(결제 완료)시 정산 생성
    @Transactional
    public void createSettlement(Order order) {
        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("결제완료 상태인 경우에 정산을 생성할 수 있어요");
        }
        User seller = userRepository.findById(order.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없어요"));
        if (seller.getRole() != UserRole.SELLER) {
            throw new IllegalArgumentException("유효한 판매자가 아닙니다");
        }
        BigDecimal fee = gradeService.calculateFeeforUser(BigDecimal.valueOf(order.getTotalAmount()), seller);
        BigDecimal refundAmount = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.valueOf(order.getTotalAmount()).multiply(BigDecimal.valueOf(0.1));

        BigDecimal totalAmount = BigDecimal.valueOf(order.getTotalAmount());
        BigDecimal settlementAmount = totalAmount.subtract(fee).subtract(refundAmount);

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