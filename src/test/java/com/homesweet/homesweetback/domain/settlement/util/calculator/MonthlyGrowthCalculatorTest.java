package com.homesweet.homesweetback.domain.settlement.util.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("전월 대비 증감률 계산")
class MonthlyGrowthCalculatorTest {
    @InjectMocks
    private MonthlyGrowthCalculator monthlyGrowthCalculator;

    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        @DisplayName("증가율 계산 - 정상 증가율")
        void growthCalculate_success_normalIncrease() {
            BigDecimal prev = BigDecimal.valueOf(100);
            BigDecimal curr = BigDecimal.valueOf(150);

            double result = monthlyGrowthCalculator.growthCalculate(prev, curr);

            assertThat(result).isEqualTo(50.0);
        }
        @Test
        @DisplayName("감소율 계산 - 음수 증가율 반환")
        void growthCalculate_success_negativeGrowth() {
            BigDecimal prev = BigDecimal.valueOf(200);
            BigDecimal curr = BigDecimal.valueOf(100);

            double result = monthlyGrowthCalculator.growthCalculate(prev, curr);

            assertThat(result).isEqualTo(-50.0);
        }
        @Test
        @DisplayName("증가율 0% (변화 없음)")
        void growthCalculate_success_zeroGrowth() {
            BigDecimal prev = BigDecimal.valueOf(100);
            BigDecimal curr = BigDecimal.valueOf(100);

            double result = monthlyGrowthCalculator.growthCalculate(prev, curr);

            assertThat(result).isEqualTo(0.0);
        }
        @Test
        @DisplayName("prevTotal = null → 증가율 0.0")
        void growthCalculate_success_prevNull() {
            BigDecimal curr = BigDecimal.valueOf(100);

            double result = monthlyGrowthCalculator.growthCalculate(null, curr);

            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("prevTotal = 0 → 증가율 0.0")
        void growthCalculate_success_prevZero() {
            BigDecimal prev = BigDecimal.ZERO;
            BigDecimal curr = BigDecimal.valueOf(100);

            double result = monthlyGrowthCalculator.growthCalculate(prev, curr);

            assertThat(result).isEqualTo(0.0);
        }
    }
    @Nested
    @DisplayName("실패 케이스")
    class Failure {
        @Test
        @DisplayName("currTotal이 null이면 NPE 발생")
        void growthCalculate_fail_currNull() {
            BigDecimal prev = BigDecimal.valueOf(100);

            assertThatThrownBy(() -> monthlyGrowthCalculator.growthCalculate(prev, null))
                    .isInstanceOf(NullPointerException.class);
        }
        @Test
        @DisplayName("currTotal이 음수면 음수 증가율 반환")
        void growthCalculate_fail_currNegative() {
            BigDecimal prev = BigDecimal.valueOf(100);
            BigDecimal curr = BigDecimal.valueOf(-50);

            double result = monthlyGrowthCalculator.growthCalculate(prev, curr);

            assertThat(result).isEqualTo(-150.0); // -150%
        }
        @Test
        @DisplayName("prevTotal이 음수면 증가율도 음수로 계산됨")
        void growthCalculate_fail_prevNegative() {
            BigDecimal prev = BigDecimal.valueOf(-100);
            BigDecimal curr = BigDecimal.valueOf(50);

            double result = monthlyGrowthCalculator.growthCalculate(prev, curr);

            assertThat(result).isEqualTo(-150.0);
        }
        @Test
        @DisplayName("prevTotal=0 & currTotal=0 → 증가율 0.0")
        void growthCalculate_edge_zeroBoth() {
            BigDecimal prev = BigDecimal.ZERO;
            BigDecimal curr = BigDecimal.ZERO;

            double result = monthlyGrowthCalculator.growthCalculate(prev, curr);

            assertThat(result).isEqualTo(0.0);
        }
    }
}