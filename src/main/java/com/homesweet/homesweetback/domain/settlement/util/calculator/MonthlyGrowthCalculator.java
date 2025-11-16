package com.homesweet.homesweetback.domain.settlement.util.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

// 전월 대비 증감률 계산
@Component
public class MonthlyGrowthCalculator {
    public double growthCalculate(BigDecimal prevTotal, BigDecimal currTotal) {
        if (prevTotal == null || prevTotal.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        } else {
            return currTotal.subtract(prevTotal)
                    .divide(prevTotal, 1, RoundingMode.HALF_UP) // 소수 1자리
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }
    }
}