package com.homesweet.homesweetback.domain.settlement.batch.step.aggregate;

import com.homesweet.homesweetback.domain.settlement.aggregate.SettlementAggregator;
import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.calculator.WeeklyDateRangeCalculator;
import com.homesweet.homesweetback.domain.settlement.util.saver.SettlementSaver;
import com.homesweet.homesweetback.domain.settlement.util.vo.SettlementTotals;
import com.homesweet.homesweetback.domain.settlement.validation.SettlementValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class WeeklySettlementTasklet implements Tasklet {
    private final SettlementRepository settlementRepository;
    private final SettlementValidator settlementValidator;
    private final SettlementAggregator settlementAggregator;
    private final DailySettlementRepository dailySettlementRepository;
    private final SettlementSaver settlementSaver;

    @Value("#{jobParameters['cutoff']}")
    private String cutoffString;

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {
        // 1. 주별 계산
        LocalDate cutoff = LocalDateTime.parse(cutoffString).toLocalDate();
        LocalDate weekStart = WeeklyDateRangeCalculator.monday(cutoff);
        LocalDate weekEnd = WeeklyDateRangeCalculator.sunday(cutoff);
        log.info("WeeklySettlementTasklet 시작: {} ~ {}", weekStart, weekEnd);

        // 2. 정산 대상 사용자 목록 조회
        List<Long> userIds = settlementRepository.findDistinctUserIds();
        for (Long userId : userIds) {
            // 3. 일별 정산 데이터 조회
            List<DailySettlement> settlements = dailySettlementRepository.findByDailySettlement(userId);
            log.info("[주별 집계] userId= {} 조회된 정산 건수={}", userId, settlements.size());
            // 4. 검증
            settlementValidator.validateWeekly(settlements);
            if (settlements.isEmpty()) {
                log.info("[주별 집계] userId= {} {} 데이터 없음", userId, cutoff);
            }
            // 5. 주 기준으로 그룹핑 + 합산
            Map<LocalDate, SettlementTotals> weeklyTotalsMap =
                    settlementAggregator.aggregate(
                            settlements,
                            d -> WeeklyDateRangeCalculator.monday(d.getSettlementDate().toLocalDate()),
                            d -> new SettlementTotals(
                                    d.getTotalSales(),
                                    d.getTotalFee(),
                                    d.getTotalVat(),
                                    d.getTotalRefund(),
                                    d.getTotalSettlement()
                            )
                    );
            // 6. upsert(저장)
            weeklyTotalsMap.forEach((date, totals) -> {
                settlementSaver.saveWeekly(userId, date, totals);
            });
            log.info("[주별 집계] userId= {} {} ~ {} 정산 {}건 완료",  userId, weekStart, weekEnd, settlements.size());
        }
        log.info("WeeklySettlementTasklet 성공");
        return RepeatStatus.FINISHED;
    }
}
