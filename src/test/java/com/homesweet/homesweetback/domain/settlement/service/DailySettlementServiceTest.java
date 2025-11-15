package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.settlement.aggregate.SettlementAggregator;
import com.homesweet.homesweetback.domain.settlement.data.HelperData;
import com.homesweet.homesweetback.domain.settlement.dto.response.DailySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.mapper.SettlementMapper;
import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.EmptyResponse;
import com.homesweet.homesweetback.domain.settlement.util.SettlementStatusUpdater;
import com.homesweet.homesweetback.domain.settlement.util.calculator.SettlementCalculator;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("일별 집계 서비스 테스트")
class DailySettlementServiceTest {

    @InjectMocks
    private DailySettlementService dailySettlementService;

    @Mock
    private DailySettlementRepository dailySettlementRepository;

    @Mock
    private EmptyResponse emptyResponse;

    @Mock
    private SettlementMapper settlementMapper;

    @Mock
    private SettlementCalculator settlementCalculator;

    @Mock
    private SettlementAggregator settlementAggregator;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private SettlementValidator settlementValidator;

    @Mock
    private SettlementSaver settlementSaver;

    @Mock
    private SettlementStatusUpdater settlementStatusUpdater;

    @Test
    @DisplayName("[성공] 일별 데이터가 존재하면 일별 response 조회")
    void getDailySummary_Success() {
        // given
        Long userId = 1L;
        LocalDate startDate = LocalDate.of(2025, 11, 10);
        LocalDate endDate = LocalDate.of(2025, 11, 11);
        Pageable pageable = PageRequest.of(0, 10);

        DailySettlement dailySettlement = HelperData.getDailySettlement();
        Page<DailySettlement> page = new PageImpl<>(List.of(dailySettlement));
        // 정산 통계용 mock
        SettlementCalculator.SettlementStats stats = new SettlementCalculator.SettlementStats(10L, 8L, 80.0);
        // mapper 반환 객체
        DailySettlementResponse dailySettlementResponse = HelperData.getDailySettlementResponse();
        // 동작
        given(dailySettlementRepository.findByDailySettlementByRange(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class))).willReturn(page);
        given(settlementCalculator.calculateStats(anyLong(), any(LocalDate.class), any(LocalDate.class))).willReturn(stats);
//        given(dailySettlementMapper.toDailySettlementResponse(any(DailySettlement.class), any())).willReturn(dailySettlementResponse);
//        given(emptyDailyResponse.createEmptyDaily(any(), any())).willReturn(Page.empty());
        given(settlementMapper.toDailySettlementResponseList(anyList(), any()))
                .willReturn(List.of(dailySettlementResponse));

        // when
        Page<DailySettlementResponse> result = dailySettlementService.getDailySummary(userId, startDate, endDate, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).settlementStatus()).isEqualTo(dailySettlementResponse.settlementStatus());
        assertThat(result.getContent().get(0).totalSales()).isEqualTo(dailySettlementResponse.totalSales());
    }

    @Test
    @DisplayName("[성공] 일별 집계가 날짜 기준으로 계산된다.")
    void getSettlement() {
        // given
        Long userId = 1L;
        LocalDateTime start = LocalDateTime.of(2025, 11, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 11, 11, 0, 0);

        Settlement s1 = HelperData.getSettlementWithDate(LocalDate.of(2025, 11, 10));
        Settlement s2 = HelperData.getSettlementWithDate(LocalDate.of(2025, 11, 11));
        List<Settlement> settlements = List.of(s1, s2);

        Map<LocalDate, SettlementTotals> aggregated = Map.of(
                LocalDate.of(2025, 11, 10), SettlementTotals.empty(),
                LocalDate.of(2025, 11, 11), SettlementTotals.empty()
        );

        // repository
        given(settlementRepository.findBySettlementDateRange(userId, start, end))
                .willReturn(settlements);

        // validator
        doNothing().when(settlementValidator).validateDaily(settlements);

        // aggregator – 핵심!!
        given(settlementAggregator.aggregate(
                anyList(),
                any(),
                any()
        )).willReturn((Map) aggregated);

        // when
        dailySettlementService.getSettlement(userId, start, end);

        // then
        // aggregator 호출 여부 확인 → 커버리지에 필수
        verify(settlementAggregator, times(1))
                .aggregate(anyList(), any(), any());

        // saveDaily 두 번 호출
        verify(settlementSaver, times(2))
                .saveDaily(eq(userId), any(LocalDate.class), any(SettlementTotals.class));

        // 마지막 상태 업데이트
        verify(settlementStatusUpdater, times(1))
                .markDailyCompleted(userId, start, end);
    }

    @Nested
    @DisplayName("실패 케이스")
    class Fail {
        @Test
        @DisplayName("일별 데이터가 없다면 빈 응답 반환")
        void getDailySummary_EmptyData() {
            Long userId = 1L;
            LocalDate startDate = LocalDate.of(2025, 11, 10);
            LocalDate endDate = LocalDate.of(2025, 11, 11);
            Pageable pageable = PageRequest.of(0, 10);
            Page<DailySettlement> emptyPage = Page.empty(pageable);

            DailySettlementResponse emptyDaily = HelperData.emptyDailySettlementResponse(startDate);

            Page<DailySettlementResponse> page = new PageImpl<>(List.of(emptyDaily), pageable, 1);

            // mapper 반환 빈객체
            DailySettlementResponse emptyDailySettlementResponse = HelperData.emptyDailySettlementResponse(startDate);
            // 동작
            given(dailySettlementRepository.findByDailySettlementByRange(eq(userId), any(), any(), eq(pageable))).willReturn(emptyPage);
            given(emptyResponse.createEmptyDaily(eq(startDate), eq(pageable))).willReturn(page);

            // when
            Page<DailySettlementResponse> result = dailySettlementService.getDailySummary(userId, startDate, endDate, pageable);

            assertThat(result.getContent().get(0).settlementStatus()).isEqualTo("CANCELED");
        }

        @Test
        @DisplayName("[실패] 정산 데이터가 없으면 BusinessException 발생")
        void getSettlement_Fail_NoSettlements() {
            // given
            Long userId = 1L;
            LocalDateTime start = LocalDateTime.of(2025, 11, 10, 0, 0);
            LocalDateTime end = LocalDateTime.of(2025, 11, 11, 0, 0);

            // repository는 빈 리스트 반환
            given(settlementRepository.findBySettlementDateRange(userId, start, end))
                    .willReturn(List.of());

            // validator가 예외 던짐 (실제 동작 그대로)
            doThrow(new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND))
                    .when(settlementValidator).validateDaily(List.of());

            // when & then
            assertThatThrownBy(() -> dailySettlementService.getSettlement(userId, start, end))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.SETTLEMENT_NOT_FOUND.getMessage());

            // ★ 실패 시 절대 save / status update가 실행되면 안 됨
            verify(settlementSaver, never()).saveDaily(any(), any(), any());
            verify(settlementStatusUpdater, never()).markDailyCompleted(any(), any(), any());
        }

        @Test
        @DisplayName("[실패] aggregator가 null 반환 시 예외 발생")
        void getSettlement_Fail_AggregatorReturnsNull() {
            // given
            Long userId = 1L;
            LocalDateTime start = LocalDateTime.of(2025, 11, 10, 0, 0);
            LocalDateTime end = LocalDateTime.of(2025, 11, 11, 0, 0);

            Settlement s1 = HelperData.getSettlementWithDate(LocalDate.of(2025, 11, 10));
            List<Settlement> settlements = List.of(s1);

            given(settlementRepository.findBySettlementDateRange(userId, start, end))
                    .willReturn(settlements);

            doNothing().when(settlementValidator).validateDaily(settlements);

            // aggregator가 null을 반환하도록
            given(settlementAggregator.aggregate(
                    anyList(),
                    any(),     // keyExtractor
                    any()      // totalsMapper
            )).willReturn(null);

            // when & then
            assertThatThrownBy(() -> dailySettlementService.getSettlement(userId, start, end))
                    .isInstanceOf(NullPointerException.class);

            verify(settlementSaver, never()).saveDaily(any(), any(), any());
            verify(settlementStatusUpdater, never()).markDailyCompleted(any(), any(), any());
        }

        @Test
        @DisplayName("[실패] dailySettlementSaver.saveDaily 중 예외 발생")
        void getSettlement_Fail_SaveError() {
            // given
            Long userId = 1L;
            LocalDateTime start = LocalDateTime.of(2025, 11, 10, 0, 0);
            LocalDateTime end = LocalDateTime.of(2025, 11, 11, 0, 0);

            Settlement s1 = HelperData.getSettlementWithDate(LocalDate.of(2025, 11, 10));
            List<Settlement> settlements = List.of(s1);

            Map<LocalDate, SettlementTotals> map = Map.of(
                    LocalDate.of(2025, 11, 10), SettlementTotals.empty()
            );

            given(settlementRepository.findBySettlementDateRange(userId, start, end))
                    .willReturn(settlements);

            doNothing().when(settlementValidator).validateDaily(settlements);
            given(settlementAggregator.aggregate(anyList(), any(), any()))
                    .willReturn((Map) map);

            // save에서 예외 발생시키기
            doThrow(new RuntimeException("DB error"))
                    .when(settlementSaver).saveDaily(any(), any(), any());

            // when & then
            assertThatThrownBy(() -> dailySettlementService.getSettlement(userId, start, end))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");

            verify(settlementStatusUpdater, never()).markDailyCompleted(any(), any(), any());
        }

        @Test
        @DisplayName("[실패] markDailyCompleted 중 예외 발생")
        void getSettlement_Fail_StatusUpdateError() {
            // given
            Long userId = 1L;
            LocalDateTime start = LocalDateTime.of(2025, 11, 10, 0, 0);
            LocalDateTime end = LocalDateTime.of(2025, 11, 11, 0, 0);

            Settlement s1 = HelperData.getSettlementWithDate(LocalDate.of(2025, 11, 10));
            List<Settlement> settlements = List.of(s1);

            Map<LocalDate, SettlementTotals> map = Map.of(
                    LocalDate.of(2025, 11, 10), SettlementTotals.empty()
            );

            given(settlementRepository.findBySettlementDateRange(userId, start, end))
                    .willReturn(settlements);

            doNothing().when(settlementValidator).validateDaily(settlements);
            given(settlementAggregator.aggregate(anyList(),any(), any()))
                    .willReturn((Map) map);

            doNothing().when(settlementSaver).saveDaily(any(), any(), any());

            // 상태 변경에서 예외
            doThrow(new RuntimeException("Update error"))
                    .when(settlementStatusUpdater).markDailyCompleted(userId, start, end);

            // when & then
            assertThatThrownBy(() -> dailySettlementService.getSettlement(userId, start, end))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Update error");
        }
    }
}
