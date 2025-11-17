package com.homesweet.homesweetback.domain.settlement.util.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class MonthlyDateRangeCalculatorTest {

    @Test
    @DisplayName("월별 Range 계산 성공 - 동일 월")
    void calculate_same_month() {
        MonthlyDateRangeCalculator calc = new MonthlyDateRangeCalculator();

        LocalDate start = LocalDate.of(2025, 3, 10);
        LocalDate end   = LocalDate.of(2025, 3, 25);

        MonthlyDateRangeCalculator.MonthlyDateRange range =
                calc.MonthlyDateRangeCalculate(start, end);

        assertThat(range.fromYM()).isEqualTo(YearMonth.of(2025, 3));
        assertThat(range.toYM()).isEqualTo(YearMonth.of(2025, 3));

        assertThat(range.from()).isEqualTo(LocalDateTime.of(2025, 3, 1, 0, 0));
        assertThat(range.toExclusive()).isEqualTo(LocalDateTime.of(2025, 4, 1, 0, 0));

        assertThat(range.fromYear()).isEqualTo((short) 2025);
        assertThat(range.fromMonth()).isEqualTo((byte) 3);
        assertThat(range.toYear()).isEqualTo((short) 2025);
        assertThat(range.toMonth()).isEqualTo((byte) 3);
    }

}
