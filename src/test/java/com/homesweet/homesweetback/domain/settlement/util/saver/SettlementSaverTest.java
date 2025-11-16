package com.homesweet.homesweetback.domain.settlement.util.saver;

import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.MonthlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.WeeklySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.YearlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.vo.SettlementTotals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("upsert ")
class SettlementSaverTest {

    @InjectMocks
    private SettlementSaver settlementSaver;

    @Mock
    private DailySettlementRepository dailySettlementRepository;

    @Mock
    private WeeklySettlementRepository weeklySettlementRepository;

    @Mock
    private MonthlySettlementRepository monthlySettlementRepository;

    @Mock
    private YearlySettlementRepository yearlySettlementRepository;

    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        @DisplayName("일별 집계에서 정상적으로 upsert 호출합니다.")
        void saveDaily() {
            // given
            Long userId = 1L;
            LocalDate date = LocalDate.of(2025, 11, 10);
            SettlementTotals totals = SettlementTotals.empty();

            // when
            settlementSaver.saveDaily(userId, date, totals);

            // then
            verify(dailySettlementRepository, times(1))
                    .upsertDaily(
                            eq(userId),
                            eq(date.atStartOfDay()),
                            eq(totals.getTotalSales()),
                            eq(totals.getTotalFee()),
                            eq(totals.getTotalVat()),
                            eq(totals.getTotalRefund()),
                            eq(totals.getTotalSettlement())
                    );
        }
        @Test
        @DisplayName("[성공] 정상적으로 weekly upsert가 수행된다")
        void saveWeekly_success() {
            // given
            Long userId = 1L;
            LocalDate weekStart = LocalDate.of(2025, 11, 3);
            LocalDate expectedWeekEnd = weekStart.plusDays(6);

            SettlementTotals totals = new SettlementTotals(
                    BigDecimal.valueOf(100000),
                    BigDecimal.valueOf(5000),
                    BigDecimal.valueOf(10000),
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(85000)
            );
            // when
            settlementSaver.saveWeekly(userId, weekStart, totals);
            // then
            verify(weeklySettlementRepository, times(1)).upsertWeekly(
                    eq(userId),
                    eq((short) 2025),
                    eq((byte) 11),
                    eq(weekStart),
                    eq(expectedWeekEnd),
                    eq(BigDecimal.valueOf(100000)),
                    eq(BigDecimal.valueOf(5000)),
                    eq(BigDecimal.valueOf(10000)),
                    eq(BigDecimal.ZERO),
                    eq(BigDecimal.valueOf(85000))
            );
        }

        @Test
        @DisplayName("[성공] saveMonthly 정상 호출")
        void saveMonthly_success() {
            // given
            YearMonth ym = YearMonth.of(2025, 3);
            SettlementTotals totals = new SettlementTotals(
                    BigDecimal.valueOf(100000),
                    BigDecimal.valueOf(5000),
                    BigDecimal.valueOf(10000),
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(85000)
            );
            // when
            settlementSaver.saveMonthly(1L, ym, totals);

            // then
            verify(monthlySettlementRepository, times(1))
                    .upsertMonthly(
                            eq(1L),
                            eq((short) 2025),
                            eq((byte) 3),
                            eq(totals.getTotalSales()),
                            eq(totals.getTotalFee()),
                            eq(totals.getTotalVat()),
                            eq(totals.getTotalRefund()),
                            eq(totals.getTotalSettlement())
                    );
        }
        @Test
        @DisplayName("[성공] saveYearly 정상 호출 - upsertYearly 1회 실행")
        void saveYearly_success() {
            Long userId = 1L;
            Short year = 2025;

            SettlementTotals totals = new SettlementTotals(
                    BigDecimal.valueOf(100000),
                    BigDecimal.valueOf(5000),
                    BigDecimal.valueOf(10000),
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(85000)
            );
            settlementSaver.saveYearly(userId, year, totals);

            verify(yearlySettlementRepository, times(1))
                    .upsertYearly(
                            eq(userId),
                            eq(year),
                            eq(totals.getTotalSales()),
                            eq(totals.getTotalFee()),
                            eq(totals.getTotalVat()),
                            eq(totals.getTotalRefund()),
                            eq(totals.getTotalSettlement())
                    );
        }
    }
    @Nested
    @DisplayName("실패 케이스")
    class Failure {
        @Test
        @DisplayName("[실패] Repository가 예외를 던지면 saveDaily도 예외를 전달한다")
        void saveDaily_Failure_RepositoryException() {
            // given
            Long userId = 1L;
            LocalDate date = LocalDate.of(2025, 11, 10);
            SettlementTotals totals = SettlementTotals.empty();

            doThrow(new RuntimeException("DB ERROR"))
                    .when(dailySettlementRepository)
                    .upsertDaily(anyLong(), any(), any(), any(), any(), any(), any());

            // when & then
            assertThatThrownBy(() -> settlementSaver.saveDaily(userId, date, totals))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB ERROR");
        }
        @Test
        @DisplayName("[실패] totals가 null이면 NullPointerException 발생")
        void saveWeekly_fail_totalsNull() {
            Long userId = 1L;
            LocalDate weekStart = LocalDate.of(2025, 11, 3);

            assertThatThrownBy(() ->
                    settlementSaver.saveWeekly(userId, weekStart, null)
            ).isInstanceOf(NullPointerException.class);
        }
        @Test
        @DisplayName("[실패] weekStartDate가 null이면 NullPointerException 발생")
        void saveWeekly_fail_weekStartNull() {
            SettlementTotals totals = SettlementTotals.empty();

            assertThatThrownBy(() ->
                    settlementSaver.saveWeekly(1L, null, totals)
            ).isInstanceOf(NullPointerException.class);
        }
        @Test
        @DisplayName("[실패] upsertWeekly에서 예외가 발생하면 그대로 전파된다")
        void saveWeekly_fail_repositoryThrowsException() {
            Long userId = 1L;
            LocalDate weekStart = LocalDate.of(2025, 11, 3);
            SettlementTotals totals = SettlementTotals.empty();
            doThrow(new RuntimeException("DB error"))
                    .when(weeklySettlementRepository)
                    .upsertWeekly(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

            assertThatThrownBy(() ->
                    settlementSaver.saveWeekly(userId, weekStart, totals)
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");
        }
        @Test
        @DisplayName("[실패] totals 가 null 이면 NPE 발생")
        void saveMonthly_fail_totalsNull() {
            YearMonth ym = YearMonth.of(2025, 3);
            assertThatThrownBy(() ->
                    settlementSaver.saveMonthly(1L, ym, null)
            ).isInstanceOf(NullPointerException.class);

            verify(monthlySettlementRepository, never())
                    .upsertMonthly(any(), any(), any(), any(), any(), any(), any(), any());
        }
        @Test
        @DisplayName("[실패] SettlementTotals 내부 필드가 null이어도 저장 시도 (현재 구현 기준)")
        void saveMonthly_fail_totalsFieldNull() {
            YearMonth ym = YearMonth.of(2025, 3);
            SettlementTotals totals = new SettlementTotals(
                    null,  // ← 일부러 null
                    BigDecimal.valueOf(5000),
                    BigDecimal.valueOf(10000),
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(85000)
            );
            // when
            // 예외가 발생하지 않아야 하므로 assertDoesNotThrow 사용
            assertDoesNotThrow(() ->
                    settlementSaver.saveMonthly(1L, ym, totals)
            );

            // then ⇒ repository 호출은 이루어져야 한다.
            verify(monthlySettlementRepository, times(1))
                    .upsertMonthly(
                            eq(1L),
                            eq((short) 2025),
                            eq((byte) 3),
                            isNull(), // totalSales NULL 그대로 전달됨
                            eq(BigDecimal.valueOf(5000)),
                            eq(BigDecimal.valueOf(10000)),
                            eq(BigDecimal.ZERO),
                            eq(BigDecimal.valueOf(85000))
                    );
        }
        @Test
        @DisplayName("[실패] repository.upsertMonthly 에서 예외 발생")
        void saveMonthly_fail_repositoryThrows() {
            YearMonth ym = YearMonth.of(2025, 3);
            SettlementTotals totals = new SettlementTotals(
                    BigDecimal.valueOf(100000),
                    BigDecimal.valueOf(5000),
                    BigDecimal.valueOf(10000),
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(85000)
            );

            doThrow(new RuntimeException("DB ERROR"))
                    .when(monthlySettlementRepository)
                    .upsertMonthly(any(), any(), any(), any(), any(), any(), any(), any());

            assertThatThrownBy(() ->
                    settlementSaver.saveMonthly(1L, ym, totals)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessage("DB ERROR");
        }
        @Test
        @DisplayName("[실패] totals 가 null이면 NPE 발생")
        void saveYearly_fail_totalsNull() {
            Long userId = 1L;
            assertThatThrownBy(() ->
                    settlementSaver.saveYearly(userId, (short)2025, null)
            ).isInstanceOf(NullPointerException.class);

            verify(yearlySettlementRepository, never())
                    .upsertYearly(any(), any(), any(), any(), any(), any(), any());
        }
        @Test
        @DisplayName("[실패] totals 내부 필드가 null이어도 저장 호출은 이루어진다 → 현재 구조에서는 예외 없음")
        void saveYearly_fail_totalsFieldNull() {
            SettlementTotals totals = new SettlementTotals(
                    null,
                    BigDecimal.valueOf(5000),
                    BigDecimal.valueOf(10000),
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(85000)
            );
            // when
            settlementSaver.saveYearly(1L, (short) 2025, totals);

            // then: 호출되었는지 확인
            verify(yearlySettlementRepository, times(1))
                    .upsertYearly(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("[실패] repository 내부 에러 발생 시 예외 전파")
        void saveYearly_fail_repositoryException() {
            SettlementTotals totals = new SettlementTotals(
                    BigDecimal.TEN,
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    BigDecimal.ZERO,
                    BigDecimal.ONE
            );

            doThrow(new RuntimeException("DB error"))
                    .when(yearlySettlementRepository)
                    .upsertYearly(any(), any(), any(), any(), any(), any(), any());

            assertThatThrownBy(() ->
                    settlementSaver.saveYearly(1L, (short)2025, totals)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");
        }

    }
}