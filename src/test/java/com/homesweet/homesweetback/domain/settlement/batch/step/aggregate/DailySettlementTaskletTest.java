package com.homesweet.homesweetback.domain.settlement.batch.step.aggregate;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.grade.service.GradeService;
import com.homesweet.homesweetback.domain.settlement.aggregate.SettlementAggregator;
import com.homesweet.homesweetback.domain.settlement.data.HelperData;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.SettlementStatusUpdater;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("일별 집계 tasklet 단위테스트")
class DailySettlementTaskletTest {
    @InjectMocks
    private DailySettlementTasklet dailySettlementTasklet;

    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private SettlementValidator settlementValidator;
    @Mock
    private SettlementAggregator settlementAggregator;
    @Mock
    private SettlementSaver settlementSaver;
    @Mock
    private SettlementStatusUpdater settlementStatusUpdater;

    @Mock
    private GradeService gradeService;

    StepContribution contribution = mock(StepContribution.class);
    ChunkContext context = mock(ChunkContext.class);

    // aggregate 실제 주입
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                dailySettlementTasklet,
                "cutoffString",
                "2025-11-25T00:00:00"
        );
        SettlementCalculator calculator = new SettlementCalculator(
                gradeService, settlementRepository
        );

        SettlementAggregator aggregator = new SettlementAggregator(calculator);

        ReflectionTestUtils.setField(
                dailySettlementTasklet,   // 또는 monthlyTasklet
                "settlementAggregator",
                aggregator
        );
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        @DisplayName("정상적으로 일별 집계가 실행된다")
        void execute_success_singleUser() {
            Long userId = 10L;
            // 1. 사용자 목록 Mock
            given(settlementRepository.findDistinctUserIds())
                    .willReturn(List.of(userId));

            // 2. settlement 조회
            Settlement s1 = HelperData.getSettlementWithDate(LocalDate.now());
            given(settlementRepository.findBySettlementDateRange(
                    eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)
            )).willReturn(List.of(s1));

            doNothing().when(settlementValidator).validateDaily(anyList());

            // 3. aggregator 결과 Mock
            Map<LocalDate, SettlementTotals> aggregated = Map.of(
                    LocalDate.of(2025, 11, 26),
                    new SettlementTotals(
                            s1.getSalesAmount(),
                            s1.getFee(),
                            s1.getVat(),
                            s1.getRefundAmount(),
                            s1.getSettlementAmount()
                    )
            );

            // when
            RepeatStatus status = dailySettlementTasklet.execute(contribution, context);

            // then
            assertThat(status).isEqualTo(RepeatStatus.FINISHED);

            verify(settlementSaver, times(1))
                    .saveDaily(eq(userId), any(LocalDate.class), any(SettlementTotals.class));

            verify(settlementStatusUpdater, times(1))
                    .markDailyCompleted(
                            eq(userId),
                            any(LocalDateTime.class),
                            any(LocalDateTime.class)
                    );
        }

        @Test
        @DisplayName("여러 사용자가 있을 때 각각 집계가 실행된다")
        void execute_success_multiUsers() {

            List<Long> userIds = List.of(1L, 2L);
            given(settlementRepository.findDistinctUserIds())
                    .willReturn(userIds);

            Settlement dummy = HelperData.getSettlementWithDate(LocalDate.now());

            given(settlementRepository.findBySettlementDateRange(anyLong(), any(), any()))
                    .willReturn(List.of(dummy));

            doNothing().when(settlementValidator).validateDaily(anyList());
            dailySettlementTasklet.execute(contribution, context);

            // 사용자 2명 → saveDaily 2번 호출
            verify(settlementSaver, times(2))
                    .saveDaily(anyLong(), any(LocalDate.class), any(SettlementTotals.class));
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {
        @BeforeEach
        void removeRealAggregator() {
            // 원래 mock으로 돌아가게 설정
            ReflectionTestUtils.setField(
                    dailySettlementTasklet,
                    "settlementAggregator",
                    settlementAggregator // mock 으로 되돌림
            );
        }

        @Test
        @DisplayName("cutoffString 파싱 실패 시 DateTimeParseException 발생")
        void execute_fail_invalidCutoff() {

            ReflectionTestUtils.setField(dailySettlementTasklet, "cutoffString", "INVALID_DATE");

            assertThatThrownBy(() ->
                    dailySettlementTasklet.execute(contribution, context)
            ).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("settlementValidator.validateDaily() 에서 BusinessException 발생 시 전파됨")
        void execute_fail_validatorThrows() {

            Long userId = 10L;

            given(settlementRepository.findDistinctUserIds())
                    .willReturn(List.of(userId));

            given(settlementRepository.findBySettlementDateRange(anyLong(), any(), any()))
                    .willReturn(List.of()); // 빈 리스트

            doThrow(new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND))
                    .when(settlementValidator).validateDaily(anyList());

            assertThatThrownBy(() ->
                    dailySettlementTasklet.execute(contribution, context)
            ).isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.SETTLEMENT_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("aggregate() 가 null 반환하면 NPE 발생")
        void execute_fail_aggregateNull() {

            Long userId = 11L;

            given(settlementRepository.findDistinctUserIds())
                    .willReturn(List.of(userId));

            Settlement dummy = HelperData.getSettlementWithDate(LocalDate.now());
            given(settlementRepository.findBySettlementDateRange(anyLong(), any(), any()))
                    .willReturn(List.of(dummy));

            doNothing().when(settlementValidator).validateDaily(anyList());

            given(settlementAggregator.aggregate(anyList(), any(), any()))
                    .willReturn(null);

            assertThatThrownBy(() ->
                    dailySettlementTasklet.execute(contribution, context)
            ).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("repository 에러 발생 시 예외 전파된다")
        void execute_fail_repoError() {

            given(settlementRepository.findDistinctUserIds())
                    .willThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() ->
                    dailySettlementTasklet.execute(contribution, context)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");
        }
    }
}