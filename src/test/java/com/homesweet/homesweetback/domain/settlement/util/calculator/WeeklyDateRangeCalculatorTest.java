package com.homesweet.homesweetback.domain.settlement.util.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class WeeklyDateRangeCalculatorTest {
    @Nested
    @DisplayName("성공 케이스")
    class Success{
        @Test
        @DisplayName("같은 주일 때 주시작일 구하기")
        void getWeeklyDateRange() {
            LocalDate start = LocalDate.of(2025, 11, 10);
            LocalDate end = LocalDate.of(2025, 11, 15);
            WeeklyDateRangeCalculator.WeeklyDateRange range =
                    WeeklyDateRangeCalculator.getWeeklyDateRange(start, end);

            assertThat(range.firstWeekStart()).isEqualTo(LocalDate.of(2025, 11, 10)); // Monday
            assertThat(range.lastWeekStartEx()).isEqualTo(LocalDate.of(2025, 11, 10)); // 같은 주 Monday
            assertThat(range.week()).isEqualTo((byte) 2); // 11월 기준 2번째 주
        }
        @Test
        @DisplayName("두 주에 걸쳐 있는 날짜 범위 주시작일 계산")
        void getWeeklyDateRange_crossWeeks() {
            // given
            LocalDate start = LocalDate.of(2025, 11, 13); // Thu
            LocalDate end = LocalDate.of(2025, 11, 22);   // 다음주의 Sat

            // when
            WeeklyDateRangeCalculator.WeeklyDateRange range =
                    WeeklyDateRangeCalculator.getWeeklyDateRange(start, end);

            // then
            assertThat(range.firstWeekStart()).isEqualTo(LocalDate.of(2025, 11, 10)); // 첫 주 월요일
            assertThat(range.lastWeekStartEx()).isEqualTo(LocalDate.of(2025, 11, 17)); // 다음 주 월요일
            assertThat(range.week()).isEqualTo((byte) 2); // 첫 주 기준 주차
        }

        @Test
        @DisplayName("monday()는 월요일을 반환한다")
        void monday_success() {
            assertThat(WeeklyDateRangeCalculator.monday(LocalDate.of(2025, 11, 13)))
                    .isEqualTo(LocalDate.of(2025, 11, 10));
        }

        @Test
        @DisplayName("sunday()는 일요일을 반환한다")
        void sunday_success() {
            assertThat(WeeklyDateRangeCalculator.sunday(LocalDate.of(2025, 11, 13)))
                    .isEqualTo(LocalDate.of(2025, 11, 16));
        }
    }
    @Nested
    @DisplayName("실패 케이스")
    class getWeeklyDateRangeFail {
        @Test
        @DisplayName("startDate가 null이면 NullPointerException")
        void getWeeklyDateRange_fail_startNull() {
            // given
            LocalDate start = null;
            LocalDate end = LocalDate.of(2025, 11, 10);

            // when & then
            assertThatThrownBy(() -> WeeklyDateRangeCalculator.getWeeklyDateRange(start, end))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("endDate가 null이면 NullPointerException")
        void getWeeklyDateRange_fail_endNull() {
            // when & then
            assertThatThrownBy(() -> WeeklyDateRangeCalculator.getWeeklyDateRange(
                    LocalDate.of(2025, 11, 10), null))
                    .isInstanceOf(NullPointerException.class);
        }
        @Test
        @DisplayName("monday()에 null 전달 시 NPE 발생")
        void monday_fail_null() {
            assertThatThrownBy(() -> WeeklyDateRangeCalculator.monday(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("sunday()에 null 전달 시 NPE 발생")
        void sunday_fail_null() {
            assertThatThrownBy(() -> WeeklyDateRangeCalculator.sunday(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}