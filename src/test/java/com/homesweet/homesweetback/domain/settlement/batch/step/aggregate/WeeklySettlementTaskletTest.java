package com.homesweet.homesweetback.domain.settlement.batch.step.aggregate;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.grade.service.GradeService;
import com.homesweet.homesweetback.domain.settlement.aggregate.SettlementAggregator;
import com.homesweet.homesweetback.domain.settlement.data.HelperData;
import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("주별 tasklet 단위 테스트")
class WeeklySettlementTaskletTest {

    @InjectMocks
    private WeeklySettlementTasklet weeklySettlementTasklet;

    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private DailySettlementRepository dailySettlementRepository;
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
    void injectRealAggregator() {
        ReflectionTestUtils.setField(weeklySettlementTasklet, "cutoffString", "2025-11-25T00:00");

        SettlementCalculator calculator =
                new SettlementCalculator(gradeService, settlementRepository);

        SettlementAggregator realAggregator = new SettlementAggregator(calculator);

        // ★ 실제 aggregator 주입 (성공 케이스만 사용)
        ReflectionTestUtils.setField(
                weeklySettlementTasklet,
                "settlementAggregator",
                realAggregator
        );
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        @DisplayName("정상적으로 주별 집계가 수행된다")
        void execute_success_singleUser() {
            Long userId = 10L;
            given(settlementRepository.findDistinctUserIds())
                    .willReturn(List.of(userId));

            DailySettlement ds = HelperData.getDailySettlement();

            given(dailySettlementRepository.findByDailySettlement(userId))
                    .willReturn(List.of(ds));

            doNothing().when(settlementValidator).validateWeekly(anyList());

            Map<LocalDate, SettlementTotals> map = Map.of(
                    LocalDate.of(2025, 11, 10),
                    SettlementTotals.empty()
            );

            RepeatStatus status = weeklySettlementTasklet.execute(contribution, context);

            assertThat(status).isEqualTo(RepeatStatus.FINISHED);

            verify(settlementSaver, times(1))
                    .saveWeekly(eq(userId), eq(LocalDate.of(2025, 11, 10)), any());
        }

        @Test
        @DisplayName("여러 사용자가 있을 때 각각 집계가 수행된다")
        void execute_success_multiUsers() {
            List<Long> userIds = List.of(1L, 2L);

            given(settlementRepository.findDistinctUserIds())
                    .willReturn(userIds);

            DailySettlement ds = HelperData.getDailySettlement();
            given(dailySettlementRepository.findByDailySettlement(anyLong()))
                    .willReturn(List.of(ds));

            doNothing().when(settlementValidator).validateWeekly(anyList());

            weeklySettlementTasklet.execute(contribution, context);

            verify(settlementSaver, times(2))
                    .saveWeekly(anyLong(), any(LocalDate.class), any(SettlementTotals.class));
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {
        @BeforeEach
        void useMockAggregator() {
            // 실패 테스트에서는 mock aggregator 사용해야함
            ReflectionTestUtils.setField(
                    weeklySettlementTasklet,
                    "settlementAggregator",
                    settlementAggregator
            );
        }

        @Test
        @DisplayName("cutoffString 파싱 실패 → 예외 발생")
        void execute_fail_invalidCutoff() {

            ReflectionTestUtils.setField(
                    weeklySettlementTasklet,
                    "cutoffString",
                    "INVALID_DATE"
            );

            assertThatThrownBy(() ->
                    weeklySettlementTasklet.execute(contribution, context)
            ).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("validator.validateWeekly() 에서 BusinessException 발생")
        void execute_fail_validatorThrows() {

            Long userId = 10L;

            given(settlementRepository.findDistinctUserIds())
                    .willReturn(List.of(userId));

            given(dailySettlementRepository.findByDailySettlement(userId))
                    .willReturn(List.of());

            doThrow(new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND))
                    .when(settlementValidator).validateWeekly(anyList());

            assertThatThrownBy(() ->
                    weeklySettlementTasklet.execute(contribution, context)
            ).isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("aggregator.aggregate() 가 null → NPE 발생")
        void execute_fail_aggregateNull() {
            Long userId = 10L;

            given(settlementRepository.findDistinctUserIds())
                    .willReturn(List.of(userId));

            DailySettlement ds = HelperData.getDailySettlement();
            given(dailySettlementRepository.findByDailySettlement(userId))
                    .willReturn(List.of(ds));

            doNothing().when(settlementValidator).validateWeekly(anyList());

            given(settlementAggregator.aggregate(anyList(), any(), any()))
                    .willReturn(null);

            assertThatThrownBy(() ->
                    weeklySettlementTasklet.execute(contribution, context)
            ).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("dailySettlementRepository 에서 예외 발생")
        void execute_fail_repoError() {

            given(settlementRepository.findDistinctUserIds())
                    .willReturn(List.of(10L));

            given(dailySettlementRepository.findByDailySettlement(anyLong()))
                    .willThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() ->
                    weeklySettlementTasklet.execute(contribution, context)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB error");
        }

        @Test
        @DisplayName("saveWeekly() 중 예외 발생")
        void execute_fail_saveWeeklyError() {

            Long userId = 10L;

            given(settlementRepository.findDistinctUserIds())
                    .willReturn(List.of(userId));

            DailySettlement ds = HelperData.getDailySettlement();
            given(dailySettlementRepository.findByDailySettlement(anyLong()))
                    .willReturn(List.of(ds));

            doNothing().when(settlementValidator).validateWeekly(anyList());

            given(settlementAggregator.aggregate(anyList(), any(), any()))
                    .willReturn(Map.of(LocalDate.of(2025, 1, 6), SettlementTotals.empty()));

            doThrow(new RuntimeException("SAVE ERROR"))
                    .when(settlementSaver).saveWeekly(anyLong(), any(), any());

            assertThatThrownBy(() ->
                    weeklySettlementTasklet.execute(contribution, context)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("SAVE ERROR");
        }
    }
}