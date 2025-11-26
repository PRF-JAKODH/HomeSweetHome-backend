package com.homesweet.homesweetback.domain.settlement.batch.step.aggregate;

import com.homesweet.homesweetback.domain.settlement.aggregate.SettlementAggregator;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.SettlementStatusUpdater;
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
/**
 * 여러 settlement -> 한 건의 dailySettlement -> tasklet
 * */
public class DailySettlementTasklet implements Tasklet {
    private final SettlementRepository settlementRepository;
    private final SettlementValidator settlementValidator;
    private final SettlementAggregator settlementAggregator;
    private final SettlementSaver settlementSaver;
    private final SettlementStatusUpdater settlementStatusUpdater;

    @Value("#{jobParameters['cutoff']}")
    private String cutoffString;

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {
        // 1. 일자 계산
        LocalDate cutoffDate = LocalDateTime.parse(cutoffString).toLocalDate();
        LocalDateTime start = cutoffDate.atStartOfDay();
        LocalDateTime end = cutoffDate.plusDays(1).atStartOfDay();
        log.info("DailySettlementTasklet 시작: {}", cutoffDate);

        // 2. 정산 대상 사용자 목록 조회
        List<Long> userIds = settlementRepository.findDistinctUserIds();
        for (Long userId : userIds) {
            // 3. 하루 정산 데이터 조회
            List<Settlement> settlements = settlementRepository.findBySettlementDateRange(userId, start, end);
            log.info("[일별 집계] userId={} 조회된 정산건수={}", userId, settlements.size());
            // 4. 검증
            settlementValidator.validateDaily(settlements);
            if (settlements.isEmpty()) {
                log.info("[일별 집계] userId= {} {} 데이터 없음", userId, cutoffDate);
            }
            // 5. 일자 기준으로 그룹핑 + 합산
            Map<LocalDate, SettlementTotals> dailyTotalsMap =
                    settlementAggregator.aggregate(
                            settlements,
                            s -> s.getSettlementDate().toLocalDate(),
                            s -> new SettlementTotals(
                                    s.getSalesAmount(),
                                    s.getFee(),
                                    s.getVat(),
                                    s.getRefundAmount(),
                                    s.getSettlementAmount()
                            )
                    );

            // 6. upsert (저장)
            dailyTotalsMap.forEach((date, totals) -> {
                settlementSaver.saveDaily(userId, date, totals);
                log.info("[일별 집계] userId={} 날짜={} 총 정산금액={}", userId, date, totals.getTotalSettlement());
            });
            // 7. 정산 상태 변경 -> 'COMPLETED'
            settlementStatusUpdater.markDailyCompleted(userId, start, end);
            log.info("[일별 집계] userId={} {} 정산 {}건 완료",  userId, start, settlements.size());
        }
        log.info("DailySettlementTasklet 성공");
        return RepeatStatus.FINISHED;
    }
}