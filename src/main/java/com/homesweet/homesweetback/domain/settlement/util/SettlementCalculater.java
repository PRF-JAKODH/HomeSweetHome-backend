package com.homesweet.homesweetback.domain.settlement.util;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.grade.service.GradeService;
import com.homesweet.homesweetback.domain.order.entity.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

// 정산 금액 계산
@Component
public class SettlementCalculater {

    private final GradeService gradeService;

    public SettlementCalculater(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    public Result getResult(Order order, User seller) {
        BigDecimal fee = gradeService.calculateFeeforUser(BigDecimal.valueOf(order.getTotalAmount()), seller);
        BigDecimal refundAmount = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.valueOf(order.getTotalAmount()).multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = BigDecimal.valueOf(order.getTotalAmount());
        BigDecimal settlementAmount = totalAmount.subtract(fee).subtract(refundAmount).setScale(2, RoundingMode.HALF_UP);
        return new Result(fee, refundAmount, vat, totalAmount, settlementAmount);
    }

    public record Result(BigDecimal fee, BigDecimal refundAmount, BigDecimal vat, BigDecimal totalAmount, BigDecimal settlementAmount) {
    }
}
