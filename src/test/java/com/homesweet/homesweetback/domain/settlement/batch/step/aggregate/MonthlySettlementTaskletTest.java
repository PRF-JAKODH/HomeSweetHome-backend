package com.homesweet.homesweetback.domain.settlement.batch.step.aggregate;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.grade.service.GradeService;
import com.homesweet.homesweetback.domain.settlement.aggregate.SettlementAggregator;
import com.homesweet.homesweetback.domain.settlement.data.HelperData;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.WeeklySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.calculator.SettlementCalculator;
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
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("월별 tasklet 단위 테스트")
class MonthlySettlementTaskletTest {

    @InjectMocks
    private MonthlySettlementTasklet monthlySettlementTasklet;
    @Mock
    private WeeklySettlementRepository weeklySettlementRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private SettlementValidator settlementValidator;

    @Mock
    private SettlementAggregator settlementAggregator;

    @Mock
    private SettlementSaver settlementSaver;

    @Mock
    private GradeService gradeService;

    StepContribution contribution = mock(StepContribution.class);
    ChunkContext context = mock(ChunkContext.class);

    @BeforeEach
    // 실제 aggregator 주입용
    void injectRealAggregator() {
        ReflectionTestUtils.setField(monthlySettlementTasklet, "cutoffString", "2025-11-25T00:00:00");
        SettlementCalculator calculator = new SettlementCalculator(gradeService, settlementRepository);
        SettlementAggregator realAgg = new SettlementAggregator(calculator);

        ReflectionTestUtils.setField(monthlySettlementTasklet, "settlementAggregator", realAgg);
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success{
        @Test
        @DisplayName("단일 사용자 월별 집계 성공")
        void execute_success_singleUser() {

            Long userId = 11L;

            given(settlementRepository.findDistinctUserIds())
                    .willReturn(List.of(userId));

            WeeklySettlement w1 = HelperData.getWeeklySettlement();
            given(weeklySettlementRepository.findByWeeklySettlement(userId))
                    .willReturn(List.of(w1));

            doNothing().when(settlementValidator).validateMonthly(anyList());

            RepeatStatus status = monthlySettlementTasklet.execute(contribution, context);

            assertThat(status).isEqualTo(RepeatStatus.FINISHED);

            verify(settlementSaver, atLeastOnce())
                    .saveMonthly(eq(userId), any(YearMonth.class), any(SettlementTotals.class));
        }

        @Test
        @DisplayName("여러 사용자 월별 집계 성공")
        void execute_success_multiUsers() {

            List<Long> userIds = List.of(1L, 2L);

            given(settlementRepository.findDistinctUserIds())
                    .willReturn(userIds);

            WeeklySettlement w1 = HelperData.getWeeklySettlement();

            given(weeklySettlementRepository.findByWeeklySettlement(anyLong()))
                    .willReturn(List.of(w1));

            doNothing().when(settlementValidator).validateMonthly(anyList());

            monthlySettlementTasklet.execute(contribution, context);

            verify(settlementSaver, times(2))
                    .saveMonthly(anyLong(), any(YearMonth.class), any(SettlementTotals.class));
        }
    }
    @Nested
    @DisplayName("실패 케이스")
    class Failure{
        @BeforeEach
        void useMockAggregator() {
            // 실패 테스트에서는 mock aggregator 사용해야함
            ReflectionTestUtils.setField(
                    monthlySettlementTasklet,
                    "settlementAggregator",
                    settlementAggregator
            );
        }

        @Test
        @DisplayName("cutoffString 파싱 실패 → 예외 발생")
        void execute_fail_invalidCutoff() {

            ReflectionTestUtils.setField(
                    monthlySettlementTasklet,
                    "cutoffString",
                    "INVALID_DATE"
            );

            assertThatThrownBy(() ->
                    monthlySettlementTasklet.execute(contribution, context)
            ).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("validator.validateMonthly() 에서 BusinessException 발생")
        void execute_fail_validatorThrows() {
            Long userId = 10L;

            given(settlementRepository.findDistinctUserIds())
                    .willReturn(List.of(userId));

            given(weeklySettlementRepository.findByWeeklySettlement(userId))
                    .willReturn(List.of());

            doThrow(new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND))
                    .when(settlementValidator).validateMonthly(anyList());

            assertThatThrownBy(() ->
                    monthlySettlementTasklet.execute(contribution, context)
            ).isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("aggregator.aggregate() 가 null → NPE 발생")
        void execute_fail_aggregateNull() {
            Long userId = 10L;

            given(settlementRepository.findDistinctUserIds())
                    .willReturn(List.of(userId));

            WeeklySettlement ws = HelperData.getWeeklySettlement();
            given(weeklySettlementRepository.findByWeeklySettlement(userId))
                    .willReturn(List.of(ws));

            doNothing().when(settlementValidator).validateMonthly(anyList());

            given(settlementAggregator.aggregate(anyList(), any(), any()))
                    .willReturn(null);

            assertThatThrownBy(() ->
                    monthlySettlementTasklet.execute(contribution, context)
            ).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("dailySettlementRepository 에서 예외 발생")
        void execute_fail_repoError() {

            given(settlementRepository.findDistinctUserIds())
                    .willReturn(List.of(10L));

            given(weeklySettlementRepository.findByWeeklySettlement(anyLong()))
                    .willThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() ->
                    monthlySettlementTasklet.execute(contribution, context)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB error");
        }

        @Test
        @DisplayName("saveMonthly 중 예외 발생 시 그대로 전파된다")
        void execute_fail_saveMonthlyError() {
            Long userId = 10L;

            // 1) cutoff 설정
            ReflectionTestUtils.setField(monthlySettlementTasklet, "cutoffString", "2025-11-25T00:00:00");

            // 2) 사용자 목록
            given(settlementRepository.findDistinctUserIds())
                    .willReturn(List.of(userId));

            // 3) 주별 셋값 조회
            WeeklySettlement w = HelperData.getWeeklySettlement();
            given(weeklySettlementRepository.findByWeeklySettlement(userId))
                    .willReturn(List.of(w));

            doNothing().when(settlementValidator).validateMonthly(anyList());

            // 4) aggregator → 반드시 YearMonth key 반환해야 saveMonthly까지 진입함
            Map<YearMonth, SettlementTotals> map =
                    Map.of(YearMonth.of(2025, 11), SettlementTotals.empty());

            given(settlementAggregator.aggregate(anyList(), any(), any()))
                    .willReturn((Map) map);

            // 5) saveMonthly 에서 에러 발생하도록
            doThrow(new RuntimeException("SAVE ERROR"))
                    .when(settlementSaver).saveMonthly(anyLong(), any(), any());

            // 6) 실행 & 검증
            assertThatThrownBy(() ->
                    monthlySettlementTasklet.execute(contribution, context)
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("SAVE ERROR");
        }
    }
}