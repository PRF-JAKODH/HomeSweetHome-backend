package com.homesweet.homesweetback.domain.settlement.service.unit;

import com.homesweet.homesweetback.domain.settlement.aggregate.SettlementAggregator;
import com.homesweet.homesweetback.domain.settlement.data.HelperData;
import com.homesweet.homesweetback.domain.settlement.dto.response.YearlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.MonthlySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.YearlySettlement;
import com.homesweet.homesweetback.domain.settlement.mapper.SettlementMapper;
import com.homesweet.homesweetback.domain.settlement.repository.MonthlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.YearlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.service.YearlySettlementService;
import com.homesweet.homesweetback.domain.settlement.dto.response.EmptyResponse;
import com.homesweet.homesweetback.domain.settlement.util.calculator.SettlementCalculator;
import com.homesweet.homesweetback.domain.settlement.util.calculator.YearlyDateRangeCalculator;
import com.homesweet.homesweetback.domain.settlement.util.saver.SettlementSaver;
import com.homesweet.homesweetback.domain.settlement.util.vo.SettlementTotals;
import com.homesweet.homesweetback.domain.settlement.validation.SettlementValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("연별 서비스 테스트")
class YearlySettlementServiceTest {
    @Mock
    private MonthlySettlementRepository monthlySettlementRepository;
    @Mock
    private SettlementValidator settlementValidator;
    @Mock
    private SettlementSaver settlementSaver;
    @InjectMocks
    private YearlySettlementService yearlySettlementService;
    @Mock
    private YearlyDateRangeCalculator yearlyDateRangeCalculator;
    @Mock
    private YearlySettlementRepository yearlySettlementRepository;
    @Mock
    private EmptyResponse emptyResponse;
    @Mock
    private SettlementMapper settlementMapper;
    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private SettlementCalculator settlementCalculator;

    @Nested
    @DisplayName("성공 케이스")
    class Success {
        private YearlySettlementService yearlySettlementService;

        @BeforeEach
        void setup() {
            SettlementAggregator realAggregator = new SettlementAggregator(settlementCalculator);
            yearlySettlementService = new YearlySettlementService(
                    yearlySettlementRepository,
                    monthlySettlementRepository,
                    settlementRepository,
                    yearlyDateRangeCalculator,
                    emptyResponse,
                    settlementMapper,
                    settlementValidator,
                    realAggregator,
                    settlementSaver
            );
        }

        @Test
        @DisplayName("연별 집계가 정상적으로 수행된다")
        void getYearlySettlement_success() {
            Long userId = 1L;
            MonthlySettlement m1 = HelperData.getMonthlySettlementWithYearMonth(2025, 1);
            MonthlySettlement m2 = HelperData.getMonthlySettlementWithYearMonth(2025, 2);
            MonthlySettlement m3 = HelperData.getMonthlySettlementWithYearMonth(2026, 1);

            List<MonthlySettlement> list = List.of(m1, m2, m3);

            given(monthlySettlementRepository.findByMonthlySettlement(userId))
                    .willReturn(list);

            doNothing().when(settlementValidator).validateYearly(list);

            Map<Short, SettlementTotals> aggregated = Map.of(
                    (short) 2025, SettlementTotals.empty(),
                    (short) 2026, SettlementTotals.empty()
            );
            // when
            yearlySettlementService.getYearlySettlement(userId);

            // then
            verify(monthlySettlementRepository).findByMonthlySettlement(userId);
            verify(settlementValidator).validateYearly(list);
            verify(settlementSaver, times(2)).saveYearly(eq(userId), anyShort(), any());
        }
        @Test
        @DisplayName("연별 정산이 비어있으면 EmptyResponse 반환")
        void getYearlySummary_empty() {
            Long userId = 1L;
            LocalDate start = LocalDate.of(2024, 1, 1);
            LocalDate end = LocalDate.of(2024, 12, 31);
            Pageable pageable = PageRequest.of(0, 10);

            // Range
            YearlyDateRangeCalculator.YearlyDateRange range =
                    new YearlyDateRangeCalculator.YearlyDateRange(
                            YearMonth.of(2024, 1),                     // fromYearMonth
                            (short) 2024,                              // fromYear
                            (short) 2025,                              // toYearExclusive
                            LocalDateTime.of(2024, 1, 1, 0, 0),        // fromDateTime
                            LocalDateTime.of(2025, 1, 1, 0, 0)         // toDateTimeExclusive
                    );
            given(yearlyDateRangeCalculator.calculate(start, end))
                    .willReturn(range);

            given(yearlySettlementRepository.findByYearlySettlementByRange(
                    anyLong(), anyShort(), anyShort(), eq(pageable)
            )).willReturn(Page.empty(pageable));

            Page<YearlySettlementResponse> empty =
                    new PageImpl<>(List.of(
                            new YearlySettlementResponse(
                                    (short) 2024,
                                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                                    BigDecimal.ZERO, BigDecimal.ZERO, 0L
                            )
                    ), pageable, 0);

            given(emptyResponse.createEmptyYearly(range.fromYearMonth(), pageable))
                    .willReturn(empty);

            Page<YearlySettlementResponse> result =
                    yearlySettlementService.getYearlySummary(userId, start, end, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).totalSales()).isEqualTo(BigDecimal.ZERO);
        }
        @Test
        @DisplayName("totalCount 정상 반영")
        void getYearlySummary_totalCount_ok() {
            Long userId = 1L;
            LocalDate start = LocalDate.of(2025, 1, 1);
            LocalDate end = LocalDate.of(2025, 12, 31);
            Pageable pageable = PageRequest.of(0, 10);

            YearlyDateRangeCalculator.YearlyDateRange range =
                    new YearlyDateRangeCalculator.YearlyDateRange(
                            YearMonth.of(2025, 1),                       // fromYearMonth
                            (short) 2025,                                // fromYear
                            (short) 2026,                                // toYearExclusive
                            LocalDateTime.of(2025, 1, 1, 0, 0),          // fromDateTime
                            LocalDateTime.of(2026, 1, 1, 0, 0)           // toDateTimeExclusive
                    );

            given(yearlyDateRangeCalculator.calculate(start, end))
                    .willReturn(range);

            YearlySettlement y1 = HelperData.getYearlySettlement();

            Page<YearlySettlement> page = new PageImpl<>(List.of(y1));

            given(yearlySettlementRepository.findByYearlySettlementByRange(
                    anyLong(),
                    eq((short) 2025),
                    eq((short) 2026),
                    eq(pageable)
            )).willReturn(page);

            given(settlementRepository.countAllByOrderedAt(
                    eq(userId),
                    eq(LocalDateTime.of(2025, 1, 1, 0, 0)),
                    eq(LocalDateTime.of(2026, 1, 1, 0, 0))
            )).willReturn(123L);

            // Mapper
            given(settlementMapper.toYearlyResponses(page.getContent(), 123L))
                    .willReturn(List.of(
                            new YearlySettlementResponse(
                                    y1.getYear(),
                                    y1.getTotalSales(),
                                    y1.getTotalFee(),
                                    y1.getTotalVat(),
                                    y1.getTotalRefund(),
                                    y1.getTotalSettlement(),
                                    123L
                            )
                    ));
            // when
            Page<YearlySettlementResponse> result =
                    yearlySettlementService.getYearlySummary(userId, start, end, pageable);
            // then
            assertThat(result.getTotalElements()).isEqualTo(123L);
        }
    }
    @Nested
    @DisplayName("실패 케이스")
    class Failure {
        @Test
        @DisplayName("Range 계산 중 에러 발생 → 예외 전파")
        void getYearlySummary_fail_rangeError() {
            Long userId = 1L;
            LocalDate start = LocalDate.of(2024, 1, 1);
            LocalDate end = LocalDate.of(2024, 12, 31);
            Pageable pageable = PageRequest.of(0, 10);

            given(yearlyDateRangeCalculator.calculate(start, end))
                    .willThrow(new RuntimeException("range error"));

            assertThatThrownBy(() ->
                    yearlySettlementService.getYearlySummary(userId, start, end, pageable)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessage("range error");
        }

        @Test
        @DisplayName("yearlySettlementRepository 에러 발생")
        void getYearlySummary_fail_repo() {
            Long userId = 1L;
            LocalDate start = LocalDate.of(2024, 1, 1);
            LocalDate end = LocalDate.of(2024, 12, 31);
            Pageable pageable = PageRequest.of(0, 10);

            YearlyDateRangeCalculator.YearlyDateRange range =
                    new YearlyDateRangeCalculator.YearlyDateRange(
                            YearMonth.of(2024, 1),                  // fromYearMonth
                            (short) 2024,                           // fromYear
                            (short) 2025,                           // toYearExclusive
                            LocalDateTime.of(2024, 1, 1, 0, 0),     // fromDateTime
                            LocalDateTime.of(2025, 1, 1, 0, 0)      // toDateTimeExclusive
                    );


            given(yearlyDateRangeCalculator.calculate(start, end)).willReturn(range);

            given(yearlySettlementRepository.findByYearlySettlementByRange(
                    anyLong(), anyShort(), anyShort(), eq(pageable)))
                    .willThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() ->
                    yearlySettlementService.getYearlySummary(userId, start, end, pageable)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");
        }
        @Test
        @DisplayName("totalCount 계산 중 에러 발생")
        void getYearlySummary_fail_totalCount() {
            Long userId = 1L;
            LocalDate start = LocalDate.of(2025, 1, 1);
            LocalDate end = LocalDate.of(2025, 12, 31);
            Pageable pageable = PageRequest.of(0, 10);

            YearlyDateRangeCalculator.YearlyDateRange range =
                    new YearlyDateRangeCalculator.YearlyDateRange(
                            YearMonth.of(2025, 1),
                            (short) 2025,
                            (short) 2026,
                            LocalDateTime.of(2025, 1, 1, 0, 0),
                            LocalDateTime.of(2026, 1, 1, 0, 0)
                    );

            given(yearlyDateRangeCalculator.calculate(start, end)).willReturn(range);

            given(yearlySettlementRepository.findByYearlySettlementByRange(any(), any(), any(), eq(pageable)))
                    .willReturn(new PageImpl<>(List.of()));

            given(settlementRepository.countAllByOrderedAt(any(), any(), any()))
                    .willThrow(new RuntimeException("계산 실패"));

            assertThatThrownBy(() ->
                    yearlySettlementService.getYearlySummary(userId, start, end, pageable)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessage("계산 실패");
        }

        @Test
        @DisplayName("Mapper 가 null 반환 → IllegalArgumentException 발생")
        void getYearlySummary_fail_mapperNull() {
            Long userId = 1L;
            LocalDate start = LocalDate.of(2025, 1, 1);
            LocalDate end = LocalDate.of(2025, 12, 31);
            Pageable pageable = PageRequest.of(0, 10);

            YearlyDateRangeCalculator.YearlyDateRange range =
                    new YearlyDateRangeCalculator.YearlyDateRange(
                            YearMonth.of(2025, 1),
                            (short) 2025,
                            (short) 2026,
                            LocalDateTime.of(2025, 1, 1, 0, 0),
                            LocalDateTime.of(2026, 1, 1, 0, 0)
                    );

            given(yearlyDateRangeCalculator.calculate(start, end)).willReturn(range);

            YearlySettlement ys = HelperData.getYearlySettlement();
            Page<YearlySettlement> page = new PageImpl<>(List.of(ys));
            given(yearlySettlementRepository.findByYearlySettlementByRange(any(), any(), any(), eq(pageable)))
                    .willReturn(page);

            given(settlementRepository.countAllByOrderedAt(any(), any(), any()))
                    .willReturn(5L);

            // mapper 가 null 반환
            given(settlementMapper.toYearlyResponses(anyList(), eq(5L)))
                    .willReturn(null);

            assertThatThrownBy(() ->
                    yearlySettlementService.getYearlySummary(userId, start, end, pageable)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Content must not be null");
        }
        @Test
        @DisplayName("Mapper 내부에서 예외 발생 → 전파")
        void getYearlySummary_fail_mapperException() {
            Long userId = 1L;
            LocalDate start = LocalDate.of(2024, 1, 1);
            LocalDate end = LocalDate.of(2024, 12, 31);
            Pageable pageable = PageRequest.of(0, 10);

            YearlyDateRangeCalculator.YearlyDateRange range =
                    new YearlyDateRangeCalculator.YearlyDateRange(
                            YearMonth.of(2024, 1),                     // fromYearMonth
                            (short) 2024,                              // fromYear
                            (short) 2025,                              // toYearExclusive
                            LocalDateTime.of(2024, 1, 1, 0, 0),        // fromDateTime
                            LocalDateTime.of(2025, 1, 1, 0, 0)         // toDateTimeExclusive
                    );
            given(yearlyDateRangeCalculator.calculate(start, end)).willReturn(range);

            YearlySettlement ys = HelperData.getYearlySettlement();
            Page<YearlySettlement> page = new PageImpl<>(List.of(ys));

            given(yearlySettlementRepository.findByYearlySettlementByRange(anyLong(), anyShort(), anyShort(), eq(pageable)))
                    .willReturn(page);

            given(settlementRepository.countAllByOrderedAt(any(), any(), any())).willReturn(20L);

            given(settlementMapper.toYearlyResponses(anyList(), eq(20L)))
                    .willThrow(new RuntimeException("mapper error"));

            assertThatThrownBy(() ->
                    yearlySettlementService.getYearlySummary(userId, start, end, pageable)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessage("mapper error");
        }
    }
}