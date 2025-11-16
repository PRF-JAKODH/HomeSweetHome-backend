package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.settlement.aggregate.SettlementAggregator;
import com.homesweet.homesweetback.domain.settlement.data.HelperData;
import com.homesweet.homesweetback.domain.settlement.dto.response.MonthlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.MonthlySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.mapper.SettlementMapper;
import com.homesweet.homesweetback.domain.settlement.repository.MonthlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.WeeklySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.EmptyResponse;
import com.homesweet.homesweetback.domain.settlement.util.calculator.MonthlyDateRangeCalculator;
import com.homesweet.homesweetback.domain.settlement.util.saver.SettlementSaver;
import com.homesweet.homesweetback.domain.settlement.util.vo.SettlementTotals;
import com.homesweet.homesweetback.domain.settlement.validation.SettlementValidator;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;


import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthlySettlementServiceTest {

    @InjectMocks
    private MonthlySettlementService monthlySettlementService;

    @Mock
    private MonthlySettlementRepository monthlySettlementRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private EmptyResponse emptyResponse;

    @Mock
    private SettlementMapper settlementMapper;

    @Mock
    private MonthlyDateRangeCalculator monthlyDateRangeCalculator;

    @Mock
    private SettlementAggregator settlementAggregator;

    @Mock
    private WeeklySettlementRepository weeklySettlementRepository;

    @Mock
    private SettlementSaver settlementSaver;

    @Mock
    private SettlementValidator settlementValidator;

    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        void getMonthlySummary() {
            Long userId = 1L;
            LocalDate startDate = LocalDate.of(2025, 1, 10);
            LocalDate endDate = LocalDate.of(2025, 3, 20);
            Pageable pageable = PageRequest.of(0, 10);

            // 1. Range 계산 Mock
            MonthlyDateRangeCalculator.MonthlyDateRange range =
                    new MonthlyDateRangeCalculator.MonthlyDateRange(
                            YearMonth.of(2025, 1),
                            YearMonth.of(2025, 3),
                            LocalDateTime.of(2025, 1, 1, 0, 0),
                            LocalDateTime.of(2025, 4, 1, 0, 0),
                            (short) 2025, (byte) 1,
                            (short) 2025, (byte) 3
                    );

            given(monthlyDateRangeCalculator.MonthlyDateRangeCalculate(startDate, endDate))
                    .willReturn(range);

            // 2. 총 건수 Mock
            given(settlementRepository.countAllByOrderedAt(
                    eq(userId), eq(range.from()), eq(range.toExclusive())))
                    .willReturn(10L);

            // 3. 월별 settlement Mock
            MonthlySettlement m1 = MonthlySettlement.builder()
                    .year((short) 2025)
                    .month((byte) 1)
                    .totalSales(BigDecimal.valueOf(100000))
                    .totalFee(BigDecimal.valueOf(5000))
                    .totalVat(BigDecimal.valueOf(10000))
                    .totalRefund(BigDecimal.ZERO)
                    .totalSettlement(BigDecimal.valueOf(85000))
                    .build();

            Page<MonthlySettlement> page = new PageImpl<>(List.of(m1));
            given(monthlySettlementRepository.findByMonthlySettlementByRange(
                    eq(userId),
                    eq((short) 2025), eq((byte) 1),
                    eq((short) 2025), eq((byte) 3),
                    eq(pageable)
            )).willReturn(page);

            // 4. Mapper Mock
            MonthlySettlementResponse mapped =
                    new MonthlySettlementResponse(
                            (short) 2025, (byte) 1,
                            BigDecimal.valueOf(100000),
                            BigDecimal.valueOf(5000),
                            BigDecimal.valueOf(10000),
                            BigDecimal.ZERO,
                            BigDecimal.valueOf(85000),
                            0.0,
                            10L
                    );

            given(settlementMapper.toMonthlyResponses(page.getContent(), 10L))
                    .willReturn(List.of(mapped));

            // when
            Page<MonthlySettlementResponse> result =
                    monthlySettlementService.getMonthlySummary(userId, startDate, endDate, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).year()).isEqualTo((short) 2025);
        }

        @Test
        @DisplayName("[성공] 월별 데이터가 없으면 EmptyResponse 반환")
        void getMonthlySummary_empty() {
            Long userId = 1L;
            LocalDate start = LocalDate.of(2025, 1, 1);
            LocalDate end = LocalDate.of(2025, 1, 31);
            Pageable pageable = PageRequest.of(0, 10);

            MonthlyDateRangeCalculator.MonthlyDateRange range =
                    new MonthlyDateRangeCalculator.MonthlyDateRange(
                            YearMonth.of(2025, 1),
                            YearMonth.of(2025, 1),
                            LocalDateTime.of(2025, 1, 1, 0, 0),
                            LocalDateTime.of(2025, 2, 1, 0, 0),
                            (short) 2025, (byte) 1,
                            (short) 2025, (byte) 1
                    );

            given(monthlyDateRangeCalculator.MonthlyDateRangeCalculate(start, end))
                    .willReturn(range);

            given(monthlySettlementRepository.findByMonthlySettlementByRange(
                    anyLong(), anyShort(), anyByte(), anyShort(), anyByte(), eq(pageable)
            )).willReturn(Page.empty(pageable));

            MonthlySettlementResponse emptyRes =
                    new MonthlySettlementResponse(
                            (short) 2025, (byte) 1,
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO,
                            0.0, 0L
                    );

            given(emptyResponse.createEmptyMonthly(range.fromYM(), pageable))
                    .willReturn(new PageImpl<>(List.of(emptyRes), pageable, 0));

            // when
            Page<MonthlySettlementResponse> result =
                    monthlySettlementService.getMonthlySummary(userId, start, end, pageable);
            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).totalSales()).isEqualTo(BigDecimal.ZERO);
        }
        @Test
        @DisplayName("[성공] 월별 집계가 정상적으로 수행된다")
        void getMonthlySettlement_success() {
            Long userId = 1L;

            WeeklySettlement w1 = WeeklySettlement.builder()
                    .year((short) 2025).month((byte) 1)
                    .totalSales(BigDecimal.valueOf(100))
                    .totalFee(BigDecimal.ZERO)
                    .totalVat(BigDecimal.ZERO)
                    .totalRefund(BigDecimal.ZERO)
                    .totalSettlement(BigDecimal.valueOf(100))
                    .build();

            WeeklySettlement w2 = WeeklySettlement.builder()
                    .year((short) 2025).month((byte) 2)
                    .totalSales(BigDecimal.valueOf(200))
                    .totalFee(BigDecimal.ZERO)
                    .totalVat(BigDecimal.ZERO)
                    .totalRefund(BigDecimal.ZERO)
                    .totalSettlement(BigDecimal.valueOf(200))
                    .build();

            List<WeeklySettlement> settlements = List.of(w1, w2);

            // repository mocking
            given(weeklySettlementRepository.findByWeeklySettlement(userId))
                    .willReturn(settlements);

            // validator mocking
            doNothing().when(settlementValidator).validateMonthly(settlements);

            // aggregator mocking
            Map<YearMonth, SettlementTotals> aggregated = Map.of(
                    YearMonth.of(2025, 1), SettlementTotals.empty(),
                    YearMonth.of(2025, 2), SettlementTotals.empty()
            );

            given(settlementAggregator.aggregate(anyList(), any(), any()))
                    .willReturn((Map) aggregated);

            // when
            monthlySettlementService.getMonthlySettlement(userId);

            // then
            verify(weeklySettlementRepository, times(1))
                    .findByWeeklySettlement(userId);

            verify(settlementValidator, times(1))
                    .validateMonthly(settlements);

            verify(settlementAggregator, times(1))
                    .aggregate(anyList(), any(), any());

            // 저장이 월 개수만큼 호출되는지 검증 → 2번
            verify(settlementSaver, times(2))
                    .saveMonthly(eq(userId), any(YearMonth.class), any(SettlementTotals.class));
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure{
        @Test
        @DisplayName("[실패] repository 에러 발생 시 예외 전파")
        void getMonthlySummary_fail_repoError() {

            Long userId = 1L;
            LocalDate start = LocalDate.of(2025, 1, 1);
            LocalDate end = LocalDate.of(2025, 1, 31);
            Pageable pageable = PageRequest.of(0, 10);

            MonthlyDateRangeCalculator.MonthlyDateRange range =
                    new MonthlyDateRangeCalculator.MonthlyDateRange(
                            YearMonth.of(2025, 1), YearMonth.of(2025, 1),
                            LocalDateTime.now(), LocalDateTime.now(),
                            (short) 2025, (byte) 1,
                            (short) 2025, (byte) 1
                    );

            given(monthlyDateRangeCalculator.MonthlyDateRangeCalculate(start, end))
                    .willReturn(range);

            given(monthlySettlementRepository.findByMonthlySettlementByRange(
                    anyLong(), anyShort(), anyByte(), anyShort(), anyByte(), eq(pageable)
            )).willThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() ->
                    monthlySettlementService.getMonthlySummary(userId, start, end, pageable)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");
        }
        @Test
        @DisplayName("[실패] weeklySettlement 리스트가 비어있으면 예외 발생")
        void getMonthlySettlement_fail_emptyList() {
            Long userId = 1L;

            given(weeklySettlementRepository.findByWeeklySettlement(userId))
                    .willReturn(List.of());

            // Mock이지만 실제 로직 실행하도록 설정
            doCallRealMethod().when(settlementValidator).validateMonthly(anyList());

            assertThatThrownBy(() ->
                    monthlySettlementService.getMonthlySettlement(userId)
            ).isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.SETTLEMENT_NOT_FOUND.getMessage());
        }
        @Test
        @DisplayName("[실패(X)] null 요소가 포함되어도 예외가 발생하지 않아야 한다")
        void getMonthlySettlement_nullElement_shouldNotThrow() {
            Long userId = 1L;

            List<WeeklySettlement> settlements = Arrays.asList(
                    HelperData.getWeeklySettlement(),
                    null
            );
            given(weeklySettlementRepository.findByWeeklySettlement(userId))
                    .willReturn(settlements);
            // validator 실제 로직 실행
            doCallRealMethod().when(settlementValidator).validateMonthly(anyList());
            // aggregate()는 정상 Map 리턴하도록 설정
            given(settlementAggregator.aggregate(anyList(), any(), any()))
                    .willReturn(Map.of(YearMonth.of(2025, 1), SettlementTotals.empty()));

            assertThatCode(() ->
                    monthlySettlementService.getMonthlySettlement(userId)
            ).doesNotThrowAnyException();
        }
        @Test
        @DisplayName("[실패] aggregator.aggregate() 가 null 반환하면 NPE 발생")
        void getMonthlySettlement_fail_aggregateReturnsNull() {
            Long userId = 1L;
            List<WeeklySettlement> settlements = List.of(HelperData.getWeeklySettlement());

            given(weeklySettlementRepository.findByWeeklySettlement(userId))
                    .willReturn(settlements);

            doNothing().when(settlementValidator).validateMonthly(settlements);

            given(settlementAggregator.aggregate(anyList(), any(), any()))
                    .willReturn(null);

            assertThatThrownBy(() ->
                    monthlySettlementService.getMonthlySettlement(userId)
            ).isInstanceOf(NullPointerException.class);
        }
    }
}