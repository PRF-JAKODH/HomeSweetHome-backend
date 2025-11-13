package com.homesweet.homesweetback.domain.settlement.util;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.grade.service.GradeService;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

// 정산 금액 계산
@Component
public class SettlementCalculator {
    private final GradeService gradeService;
    public SettlementCalculator(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    // 정산 금액 계산
    public Result getResult(Order order, User seller) {
        Long totalSales = order.getTotalAmount();
        if (order.getTotalAmount() < 0) {
            throw new BusinessException(ErrorCode.INVALID_TOTAL_AMOUNT);
        }

        BigDecimal fee = gradeService.calculateFeeforUser(BigDecimal.valueOf(order.getTotalAmount()), seller);
        BigDecimal refundAmount = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.valueOf(order.getTotalAmount()).multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = BigDecimal.valueOf(order.getTotalAmount());
        BigDecimal settlementAmount = totalAmount.subtract(fee).subtract(refundAmount).setScale(2, RoundingMode.HALF_UP);
        return new Result(fee, refundAmount, vat, totalAmount, settlementAmount);
    }

    public record Result(BigDecimal fee, BigDecimal refundAmount, BigDecimal vat, BigDecimal totalAmount, BigDecimal settlementAmount) {
    }

    // 환불된 정산 금액 계산
    public BigDecimal refundResult(Settlement settlement) {
        BigDecimal saleAmount = settlement.getSalesAmount();
        BigDecimal fee = settlement.getFee();
        BigDecimal vat = settlement.getVat();
        BigDecimal curSettlementAmount = Optional.ofNullable(settlement.getSettlementAmount()).orElse(BigDecimal.ZERO);
        BigDecimal refundAmount = saleAmount.add(vat).subtract(fee);
        BigDecimal refundSettlementAmount = curSettlementAmount.subtract(refundAmount);
        return refundSettlementAmount.max(BigDecimal.ZERO);
    }
}
