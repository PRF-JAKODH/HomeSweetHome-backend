package com.homesweet.homesweetback.domain.settlement.mapper;

import com.homesweet.homesweetback.domain.settlement.data.HelperData;
import com.homesweet.homesweetback.domain.settlement.dto.response.DailySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.dto.response.WeeklySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.util.calculator.SettlementCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("일별 매핑 테스트")
class SettlementMapperTest {

    @InjectMocks
    private SettlementMapper settlementMapper;

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
        assertThat(dailySettlementResponse.settlementDate()).isEqualTo(LocalDate.of(2025,11,10));
        assertThat(dailySettlementResponse.settlementStatus()).isEqualTo("COMPLETED");
        assertThat(dailySettlementResponse.completedRate()).isEqualTo(100.0);
        assertThat(dailySettlementResponse.totalCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("[성공] DailySettlementResponse -> List 매핑")
    void toDailySettlementResponseList() {
        DailySettlement d1 = HelperData.getDailySettlement();
        DailySettlement d2 = HelperData.getDailySettlement();
        List<DailySettlement>list = List.of(d1, d2);

        SettlementCalculator.SettlementStats stats =
                new SettlementCalculator.SettlementStats(10L, 10L, 100.0);

        DailySettlementResponse response =
                new DailySettlementResponse(
                        d1.getTotalSales(),
                        d1.getTotalFee(),
                        d1.getTotalVat(),
                        d1.getTotalRefund(),
                        d1.getTotalSettlement(),
                        d1.getSettlementDate().toLocalDate(),
                        "COMPLETED",
                        stats.completedRate(),
                        stats.totalCount()
                );

        // stats.apply(d) 함수는 매번 stats 반환하도록 구성
        Function<DailySettlement, SettlementCalculator.SettlementStats> provider =
                (daily) -> stats;

        // when
        List<DailySettlementResponse> result =
                settlementMapper.toDailySettlementResponseList(list, provider);

        // then
        assertThat(result).hasSize(2);

        // 첫 번째 요소 검증
        assertThat(result.get(0).settlementStatus()).isEqualTo("COMPLETED");
        assertThat(result.get(0).totalCount()).isEqualTo(10L);

        // 두 번째 요소 검증
        assertThat(result.get(1).settlementStatus()).isEqualTo("COMPLETED");
        assertThat(result.get(1).totalCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("[성공] WeeklySettlement → WeeklySettlementResponse 매핑이 정상적으로 수행된다")
    void toWeeklySettlementResponse_success() {

        SettlementMapper mapper = new SettlementMapper();

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
    @DisplayName("[성공] 빈 데이터 입력 시 빈 리스트 반환")
    void toWeeklySettlementResponse_empty() {
        SettlementMapper mapper = new SettlementMapper();
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

    @Nested
    @DisplayName("실패 케이스")
    class Fail{
        @Test
        @DisplayName("리스트 매핑 실패 - DailySettlement 리스트가 비어있으면 빈 리스트 반환")
        void toDailySettlementResponseList_emptyList() {
            // given
            List<DailySettlement> emptyList = List.of();

            Function<DailySettlement, SettlementCalculator.SettlementStats> provider =
                    d -> new SettlementCalculator.SettlementStats(0L, 0L, 0.0);

            // when
            List<DailySettlementResponse> result =
                    settlementMapper.toDailySettlementResponseList(emptyList, provider);

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
            Function<DailySettlement, SettlementCalculator.SettlementStats> provider =
                    d -> new SettlementCalculator.SettlementStats(1L, 1L, 100.0);

            // when & then
            assertThatThrownBy(() ->
                    settlementMapper.toDailySettlementResponseList(list, provider)
            ).isInstanceOf(NullPointerException.class);
        }
        @Test
        @DisplayName("[실패] WeeklySettlement 리스트에 null 요소가 포함되면 NPE 발생")
        void toWeeklySettlementResponse_fail_nullElement() {
            SettlementMapper mapper = new SettlementMapper();
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
    }
}