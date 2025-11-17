package com.homesweet.homesweetback.domain.settlement.util.calculator;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.grade.service.GradeService;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.settlement.data.HelperData;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.util.vo.SettlementTotals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("정산 금액 계산 테스트")
public class SettlementCalculatorTest {
    @Mock
    private GradeService gradeService;

    @InjectMocks
    private SettlementCalculator settlementCalculator;

    @Test
    @DisplayName("[성공] 정산 금액을 계산합니다.")
    void calcSettlementAmount_Success() {
        // given
        Order order = HelperData.getOrder(HelperData.getUser());
        User seller = HelperData.getSeller(HelperData.getGrade());
        ReflectionTestUtils.setField(order, "totalAmount", 150000L);
        given(gradeService.calculateFeeforUser(BigDecimal.valueOf(150000L), seller)).willReturn(BigDecimal.valueOf(7500));

        // when
        SettlementCalculator.Result result = settlementCalculator.getResult(order, seller);

        // then
        assertThat(result).isNotNull();
        assertThat(result.vat()).isEqualTo(BigDecimal.valueOf(15000.00).setScale(2, RoundingMode.HALF_UP));
        assertThat(result.refundAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.settlementAmount()).isEqualTo(BigDecimal.valueOf(142500.00).setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("[성공] 환불된 정산 금액을 계산합니다.")
    void refundResult_Success() {
        Settlement settlement = HelperData.getSettlement();
        BigDecimal result = settlementCalculator.refundResult(settlement);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("[성공] accumulate는 DailyTotals의 add메소드를 호출합니다.")
    void accumulateDailyTotals_Success() {
        // given
        SettlementTotals totals = SettlementTotals.empty();
        Settlement settlement = HelperData.getSettlement();
        SettlementTotals mapped = new SettlementTotals(
                settlement.getSalesAmount(),
                settlement.getFee(),
                settlement.getVat(),
                settlement.getRefundAmount(),
                settlement.getSettlementAmount()
        );

        // when
        settlementCalculator.accumulate(totals, mapped);
        // then
        assertThat(totals.getTotalSales()).isEqualTo(BigDecimal.valueOf(150000));
        assertThat(totals.getTotalFee()).isEqualTo(BigDecimal.valueOf(7500));
        assertThat(totals.getTotalVat()).isEqualTo(BigDecimal.valueOf(15000));
        assertThat(totals.getTotalRefund()).isEqualTo(BigDecimal.ZERO);
        assertThat(totals.getTotalSettlement()).isEqualTo(BigDecimal.valueOf(127500));
    }

    @Test
    @DisplayName("월 초가 주 중간일 때 주시작일 계산")
    void getWeeklyDateRange_monthStartMidWeek() {
        // given
        LocalDate start = LocalDate.of(2025, 11, 1); // Saturday
        LocalDate end = LocalDate.of(2025, 11, 2);   // Sunday

        // when
        WeeklyDateRangeCalculator.WeeklyDateRange range =
                WeeklyDateRangeCalculator.getWeeklyDateRange(start, end);

        // then
        // 11월 첫 번째 월요일은 2025-11-03 이어야 한다
        assertThat(range.firstWeekStart()).isEqualTo(LocalDate.of(2025, 11, 3));

        // 주차는 1주차여야 한다
        assertThat(range.week()).isEqualTo((byte) 1);
    }

    @Test
    @DisplayName("월 말이 다음 달 초로 넘어가는 주 계산")
    void getWeeklyDateRange_endMonthToNextMonth() {
        // given
        LocalDate start = LocalDate.of(2025, 11, 28); // Fri
        LocalDate end = LocalDate.of(2025, 12, 3);   // Wed

        // when
        WeeklyDateRangeCalculator.WeeklyDateRange range =
                WeeklyDateRangeCalculator.getWeeklyDateRange(start, end);

        // then
        assertThat(range.firstWeekStart()).isEqualTo(LocalDate.of(2025, 11, 24));
        assertThat(range.lastWeekStartEx()).isEqualTo(LocalDate.of(2025, 12, 1));
        assertThat(range.week()).isEqualTo((byte) 4); // 11월의 4번쨰 주
    }

    @Nested
    @DisplayName("실패 케이스")
    class CalcSettlementAmount_Failure {
        @Test
        @DisplayName("총 금액은 음수가 될 수 없습니다.")
        void calcSettlementAmount_Failure_Negative() {
            Order order = HelperData.getOrder(HelperData.getUser());
            User seller = HelperData.getSeller(HelperData.getGrade());
            ReflectionTestUtils.setField(order, "totalAmount", -150000L);

            assertThatThrownBy(() -> settlementCalculator.getResult(order, seller))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("금액은 음수가 될 수 없습니다.");
        }

        @Test
        @DisplayName("총 금액이 0이면 모든 금액이 0입니다.")
        void calcSettlementAmount_Failure_Zero() {
            Order order = HelperData.getOrder(HelperData.getUser());
            ReflectionTestUtils.setField(order, "totalAmount", 0L);
            User seller = HelperData.getSeller(HelperData.getGrade());
            given(gradeService.calculateFeeforUser(BigDecimal.ZERO, seller)).willReturn(BigDecimal.ZERO);

            SettlementCalculator.Result result = settlementCalculator.getResult(order, seller);

            assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.settlementAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("[실패] DailyTotals가 null이면 NullPointerException")
        void accumulateDailyTotals_fail_nullTotals() {
            // given
            SettlementTotals totals = null;
            Settlement settlement = HelperData.getSettlement();
            SettlementTotals mapped = new SettlementTotals(
                    settlement.getSalesAmount(),
                    settlement.getFee(),
                    settlement.getVat(),
                    settlement.getRefundAmount(),
                    settlement.getSettlementAmount()
            );
            // when & then
            assertThatThrownBy(() -> settlementCalculator.accumulate(totals, mapped))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("[실패] SettlementTotals(mapped)의 값이 null이면 NPE 발생")
        void accumulateDailyTotals_fail_nullMapped() {
            // given
            SettlementTotals totals = SettlementTotals.empty();

            // SettlementTotals 는 BigDecimal 타입이므로 null을 그대로 넣을 수 있음
            SettlementTotals mapped = new SettlementTotals(
                    null,   // salesAmount
                    null,   // fee
                    null,   // vat
                    null,   // refundAmount
                    null    // settlementAmount
            );

            // when & then
            assertThatThrownBy(() -> settlementCalculator.accumulate(totals, mapped))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
