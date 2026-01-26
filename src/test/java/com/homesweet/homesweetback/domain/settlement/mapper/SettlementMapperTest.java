package com.homesweet.homesweetback.domain.settlement.mapper;

import com.homesweet.homesweetback.domain.settlement.data.HelperData;
import com.homesweet.homesweetback.domain.settlement.dto.response.DailySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.dto.response.MonthlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.dto.response.WeeklySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.dto.response.YearlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.MonthlySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.YearlySettlement;
import com.homesweet.homesweetback.domain.settlement.util.calculator.MonthlyGrowthCalculator;
import com.homesweet.homesweetback.domain.settlement.util.calculator.SettlementCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("매핑 테스트")
class SettlementMapperTest {
    @InjectMocks
    private SettlementMapper settlementMapper;

    @Mock
    private MonthlyGrowthCalculator monthlyGrowthCalculator;

    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        @DisplayName("DailyResponse + stats + 매핑 성공")
        void toDailySettlementResponse() {
            // given
            LocalDate settlementDate = HelperData.getDailySettlement().getSettlementDate().toLocalDate();

            DailySettlement dailySettlement = HelperData.getDailySettlement();
            SettlementCalculator.SettlementStats stats = new SettlementCalculator.SettlementStats(10L, 10L, 100.0);
            // when
            DailySettlementResponse dailySettlementResponse = settlementMapper.toDailySettlementResponse(dailySettlement, stats);
            // then
            assertThat(dailySettlementResponse).isNotNull();
            assertThat(dailySettlementResponse.totalSales()).isEqualTo(BigDecimal.valueOf(1500000));
            assertThat(dailySettlementResponse.totalFee()).isEqualTo(BigDecimal.valueOf(75000));
            assertThat(dailySettlementResponse.totalVat()).isEqualTo(BigDecimal.valueOf(150000));
            assertThat(dailySettlementResponse.totalRefund()).isEqualTo(BigDecimal.ZERO);
            assertThat(dailySettlementResponse.totalSettlement()).isEqualTo(BigDecimal.valueOf(1575000));
            assertThat(dailySettlementResponse.settlementDate()).isEqualTo(LocalDate.of(2025, 11, 10));
            assertThat(dailySettlementResponse.settlementStatus()).isEqualTo("COMPLETED");
            assertThat(dailySettlementResponse.completedRate()).isEqualTo(100.0);
            assertThat(dailySettlementResponse.totalCount()).isEqualTo(10L);
        }

        @Test
        @DisplayName("DailySettlementResponse 리스트 매핑 성공")
        void toDailySettlementResponseList() {
            // given
            DailySettlement d1 = HelperData.getDailySettlement();
            DailySettlement d2 = HelperData.getDailySettlement();
            List<DailySettlement> list = List.of(d1, d2);

            // 기간 전체에 대한 계산 결과(공통 Stats)
            SettlementCalculator.SettlementStats stats =
                    new SettlementCalculator.SettlementStats(10L, 10L, 100.0);

            // when
            List<DailySettlementResponse> result =
                    settlementMapper.toDailySettlementResponseList(list, stats);

            // then
            assertThat(result).hasSize(2);

            // 첫 번째 요소 검증
            assertThat(result.get(0).settlementStatus()).isEqualTo("COMPLETED");
            assertThat(result.get(0).totalCount()).isEqualTo(10L);

            // 두 번째 요소 검증
            assertThat(result.get(1).settlementStatus()).isEqualTo("COMPLETED");
            assertThat(result.get(1).totalCount()).isEqualTo(10L);

            // 금액 필드까지 꼼꼼히 검증하면 더 좋음
            assertThat(result.get(0).totalSales()).isEqualTo(d1.getTotalSales());
            assertThat(result.get(1).totalSales()).isEqualTo(d2.getTotalSales());
        }


        @Test
        @DisplayName("WeeklySettlement → WeeklySettlementResponse 매핑이 정상적으로 수행된다")
        void toWeeklySettlementResponse_success() {
            MonthlyGrowthCalculator calc = mock(MonthlyGrowthCalculator.class);
            SettlementMapper mapper = new SettlementMapper(calc);

            // given
            WeeklySettlement w = WeeklySettlement.builder()
                    .year((short) 2025)
                    .month((byte) 11)
                    .weekStartDate(LocalDate.of(2025, 11, 10))
                    .weekEndDate(LocalDate.of(2025, 11, 16))
                    .totalSales(BigDecimal.valueOf(100000))
                    .totalFee(BigDecimal.valueOf(5000))
                    .totalVat(BigDecimal.valueOf(10000))
                    .totalRefund(BigDecimal.ZERO)
                    .totalSettlement(BigDecimal.valueOf(85000))
                    .build();
            List<WeeklySettlement> list = List.of(w);
            SettlementCalculator.SettlementStats stats =
                    new SettlementCalculator.SettlementStats(10, 5, 50.0);     // totalCount=10, completedRate=50%
            byte week = 2;
            // when
            List<WeeklySettlementResponse> responses =
                    mapper.toWeeklySettlementResponse(list, stats, week);
            // then
            assertThat(responses).hasSize(1);

            WeeklySettlementResponse res = responses.get(0);
            // year / month / week
            assertThat(res.year()).isEqualTo((short) 2025);
            assertThat(res.month()).isEqualTo((byte) 11);
            assertThat(res.week()).isEqualTo(week);

            // 날짜
            assertThat(res.weekStartDate()).isEqualTo(LocalDate.of(2025, 11, 10));
            assertThat(res.weekEndDate()).isEqualTo(LocalDate.of(2025, 11, 16));

            // 금액
            assertThat(res.totalSales()).isEqualTo(BigDecimal.valueOf(100000));
            assertThat(res.totalFee()).isEqualTo(BigDecimal.valueOf(5000));
            assertThat(res.totalVat()).isEqualTo(BigDecimal.valueOf(10000));
            assertThat(res.totalRefund()).isEqualTo(BigDecimal.ZERO);
            assertThat(res.totalSettlement()).isEqualTo(BigDecimal.valueOf(85000));

            // stats 적용 여부
            assertThat(res.completedRate()).isEqualTo(50.0);
            assertThat(res.totalCount()).isEqualTo(10);
        }

        @Test
        @DisplayName("빈 데이터 입력 시 빈 리스트 반환")
        void toWeeklySettlementResponse_empty() {
            MonthlyGrowthCalculator calc = mock(MonthlyGrowthCalculator.class);
            SettlementMapper mapper = new SettlementMapper(calc);

            // given
            List<WeeklySettlement> emptyList = List.of();
            SettlementCalculator.SettlementStats stats = new SettlementCalculator.SettlementStats(0, 0, 0.0);
            byte week = 1;

            // when
            List<WeeklySettlementResponse> responses =
                    mapper.toWeeklySettlementResponse(emptyList, stats, week);

            // then
            assertThat(responses).isEmpty();
        }

        @Test
        @DisplayName("월별 응답 매핑 + 성장률 계산이 정상적으로 수행된다")
        void toMonthlyResponses_success() {
            // given
            MonthlySettlement m1 = MonthlySettlement.builder()
                    .year((short) 2025).month((byte) 1)
                    .totalSales(BigDecimal.valueOf(100))
                    .totalFee(BigDecimal.TEN)
                    .totalVat(BigDecimal.ONE)
                    .totalRefund(BigDecimal.ZERO)
                    .totalSettlement(BigDecimal.valueOf(90))
                    .build();

            MonthlySettlement m2 = MonthlySettlement.builder()
                    .year((short) 2025).month((byte) 2)
                    .totalSales(BigDecimal.valueOf(200))
                    .totalFee(BigDecimal.TEN)
                    .totalVat(BigDecimal.ONE)
                    .totalRefund(BigDecimal.ZERO)
                    .totalSettlement(BigDecimal.valueOf(190))
                    .build();

            MonthlySettlement m3 = MonthlySettlement.builder()
                    .year((short) 2025).month((byte) 3)
                    .totalSales(BigDecimal.valueOf(300))
                    .totalFee(BigDecimal.TEN)
                    .totalVat(BigDecimal.ONE)
                    .totalRefund(BigDecimal.ZERO)
                    .totalSettlement(BigDecimal.valueOf(290))
                    .build();

            List<MonthlySettlement> list = List.of(m1, m2, m3);

            long totalCount = 10L;

            // mock growth calculator
            when(monthlyGrowthCalculator.growthCalculate(null, BigDecimal.valueOf(100)))
                    .thenReturn(0.0); // 첫 달 → prevTotal null

            when(monthlyGrowthCalculator.growthCalculate(BigDecimal.valueOf(100), BigDecimal.valueOf(200)))
                    .thenReturn(100.0);

            when(monthlyGrowthCalculator.growthCalculate(BigDecimal.valueOf(200), BigDecimal.valueOf(300)))
                    .thenReturn(50.0);

            // when
            List<MonthlySettlementResponse> responses =
                    settlementMapper.toMonthlyResponses(list, totalCount);

            // then
            assertThat(responses).hasSize(3);

            // 검증
            assertThat(responses.get(0).growthRate()).isEqualTo(0.0);
            assertThat(responses.get(1).growthRate()).isEqualTo(100.0);
            assertThat(responses.get(2).growthRate()).isEqualTo(50.0);

            assertThat(responses.get(0).totalCount()).isEqualTo(10L);
        }

        @Test
        @DisplayName("입력 리스트가 비어있으면 빈 리스트 반환")
        void toMonthlyResponses_emptyList() {
            List<MonthlySettlement> list = List.of();

            List<MonthlySettlementResponse> result =
                    settlementMapper.toMonthlyResponses(list, 0);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("YearlySettlement 리스트가 정상 변환된다")
        void toYearlyResponses_success_single() {
            YearlySettlement y = YearlySettlement.builder()
                    .year((short) 2025)
                    .totalSales(BigDecimal.valueOf(1000))
                    .totalFee(BigDecimal.valueOf(100))
                    .totalVat(BigDecimal.valueOf(50))
                    .totalRefund(BigDecimal.ZERO)
                    .totalSettlement(BigDecimal.valueOf(850))
                    .build();

            List<YearlySettlement> list = List.of(y);

            List<YearlySettlementResponse> result =
                    settlementMapper.toYearlyResponses(list, 10L);

            assertThat(result).hasSize(1);

            YearlySettlementResponse r = result.get(0);
            assertThat(r.year()).isEqualTo((short) 2025);
            assertThat(r.totalSales()).isEqualTo(BigDecimal.valueOf(1000));
            assertThat(r.totalSettlement()).isEqualTo(BigDecimal.valueOf(850));
            assertThat(r.totalCount()).isEqualTo(10L);
        }

        @Test
        @DisplayName("여러 개 YearlySettlement가 정상 변환된다")
        void toYearlyResponses_success_multiple() {
            YearlySettlement y1 = HelperData.getYearlySettlement();
            YearlySettlement y2 = HelperData.getYearlySettlement();  // 동일 구조지만 다른 내용이라고 가정

            List<YearlySettlement> list = List.of(y1, y2);

            List<YearlySettlementResponse> result =
                    settlementMapper.toYearlyResponses(list, 99L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).totalCount()).isEqualTo(99L);
            assertThat(result.get(1).totalCount()).isEqualTo(99L);
        }
    }
    @Nested
    @DisplayName("실패 케이스")
    class Fail {
        @Test
        @DisplayName("리스트 매핑 실패 - DailySettlement 리스트가 비어있으면 빈 리스트 반환")
        void toDailySettlementResponseList_emptyList() {
            // given
            List<DailySettlement> emptyList = List.of();

            // 전체 기간에 대한 계산이 0일 때의 Stats 객체
            SettlementCalculator.SettlementStats stats =
                    new SettlementCalculator.SettlementStats(0L, 0L, 0.0);

            // when
            List<DailySettlementResponse> result =
                    settlementMapper.toDailySettlementResponseList(emptyList, stats);

            // then
            assertThat(result).isEmpty();
        }


        @Test
        @DisplayName("리스트 매핑 실패 - DailySettlement 요소가 null이면 예외 발생")
        void toDailySettlementResponseList_containsNull() {
            // given
            DailySettlement d1 = HelperData.getDailySettlement();
            DailySettlement d2 = null;

            List<DailySettlement> list = new ArrayList<>();
            list.add(d1);
            list.add(null);

            SettlementCalculator.SettlementStats stats =
                    new SettlementCalculator.SettlementStats(1L, 1L, 100.0);

            // when & then
            assertThatThrownBy(() ->
                    settlementMapper.toDailySettlementResponseList(list, stats)
            ).isInstanceOf(NullPointerException.class);
        }


        @Test
        @DisplayName("WeeklySettlement 리스트에 null 요소가 포함되면 NPE 발생")
        void toWeeklySettlementResponse_fail_nullElement() {
            MonthlyGrowthCalculator calc = mock(MonthlyGrowthCalculator.class);
            SettlementMapper mapper = new SettlementMapper(calc);

            // given
            List<WeeklySettlement> list = new ArrayList<>();
            list.add(null);   // ← null 허용

            SettlementCalculator.SettlementStats stats = new SettlementCalculator.SettlementStats(0, 0, 0.0);
            byte week = 1;

            // when & then
            assertThatThrownBy(() ->
                    mapper.toWeeklySettlementResponse(list, stats, week)
            ).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("MonthlySettlement 리스트에 null 요소가 있으면 NPE 발생")
        void toMonthlyResponses_fail_nullElement() {
            List<MonthlySettlement> list = new ArrayList<>();
            list.add(null); // 문제 요소

            assertThatThrownBy(() ->
                    settlementMapper.toMonthlyResponses(list, 0)
            ).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("list 가 null이면 NPE 발생")
        void toYearlyResponses_fail_nullList() {
            assertThatThrownBy(() ->
                    settlementMapper.toYearlyResponses(null, 10L)
            ).isInstanceOf(NullPointerException.class);
        }
        @Test
        @DisplayName("list 안에 null 요소가 있으면 NPE 발생")
        void toYearlyResponses_fail_containsNull() {
            YearlySettlement y = HelperData.getYearlySettlement();
            List<YearlySettlement> list = Arrays.asList(y, null);

            assertThatThrownBy(() ->
                    settlementMapper.toYearlyResponses(list, 10L)
            ).isInstanceOf(NullPointerException.class);
        }
        @Test
        @DisplayName("YearlySettlement 리스트에 null 요소가 포함되면 NPE 발생")
        void toYearlyResponses_fail_nullElement() {
            List<YearlySettlement> list = Arrays.asList(
                    (YearlySettlement) null  // 명시적 캐스팅으로 warning 제거
            );
            assertThatThrownBy(() -> settlementMapper.toYearlyResponses(list, 10L)).isInstanceOf(NullPointerException.class);
        }
    }
}