package com.homesweet.homesweetback.domain.settlement.util.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("연도 계산")
class YearlyDateRangeCalculatorTest {
    @Nested
    @DisplayName("성공 케이스")
    class Success{
        @Test
        @DisplayName("시작연도~종료연도 Range 정상 계산")
        void calculate_success_basic() {
            YearlyDateRangeCalculator calc = new YearlyDateRangeCalculator();

            LocalDate start = LocalDate.of(2024, 5, 10);
            LocalDate end = LocalDate.of(2024, 11, 3);

            YearlyDateRangeCalculator.YearlyDateRange range = calc.calculate(start, end);

            assertThat(range.fromYear()).isEqualTo((short) 2024);
            assertThat(range.toYearExclusive()).isEqualTo((short) 2025);
            assertThat(range.fromYearMonth()).isEqualTo(YearMonth.of(2024, 1));
            assertThat(range.fromDateTime()).isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0));
            assertThat(range.toDateTimeExclusive()).isEqualTo(LocalDateTime.of(2025, 1, 1, 0, 0));
        }
        @Test
        @DisplayName("서로 다른 연도 범위 계산(2023~2025)")
        void calculate_success_multiYear() {
            YearlyDateRangeCalculator calc = new YearlyDateRangeCalculator();

            LocalDate start = LocalDate.of(2023, 6, 1);
            LocalDate end   = LocalDate.of(2025, 3, 15);

            YearlyDateRangeCalculator.YearlyDateRange range = calc.calculate(start, end);

            assertThat(range.fromYear()).isEqualTo((short) 2023);
            assertThat(range.toYearExclusive()).isEqualTo((short) 2026);
            assertThat(range.fromDateTime()).isEqualTo(LocalDateTime.of(2023, 1, 1, 0, 0));
            assertThat(range.toDateTimeExclusive()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        }
        @Test
        @DisplayName("startDate = endDate 인 경우에도 정상 처리")
        void calculate_success_sameDay() {
            YearlyDateRangeCalculator calc = new YearlyDateRangeCalculator();

            LocalDate today = LocalDate.of(2025, 8, 1);

            YearlyDateRangeCalculator.YearlyDateRange range = calc.calculate(today, today);

            assertThat(range.fromYear()).isEqualTo((short) 2025);
            assertThat(range.toYearExclusive()).isEqualTo((short) 2026);
        }
    }
    @Nested
    @DisplayName("실패 케이스")
    class Fail{
        @Test
        @DisplayName("startDate 가 null이면 NPE 발생")
        void calculate_fail_startDate_null() {
            YearlyDateRangeCalculator calc = new YearlyDateRangeCalculator();

            assertThatThrownBy(() ->
                    calc.calculate(null, LocalDate.of(2025, 1, 1))
            ).isInstanceOf(NullPointerException.class);
        }
        @Test
        @DisplayName("endDate 가 null이면 NPE 발생")
        void calculate_fail_endDate_null() {
            YearlyDateRangeCalculator calc = new YearlyDateRangeCalculator();

            assertThatThrownBy(() ->
                    calc.calculate(LocalDate.of(2025, 1, 1), null)
            ).isInstanceOf(NullPointerException.class);
        }
    }

}