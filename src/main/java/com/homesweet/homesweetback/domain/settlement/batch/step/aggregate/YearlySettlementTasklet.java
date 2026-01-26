package com.homesweet.homesweetback.domain.settlement.batch.step.aggregate;

import com.homesweet.homesweetback.domain.settlement.aggregate.SettlementAggregator;
import com.homesweet.homesweetback.domain.settlement.entity.MonthlySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.MonthlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
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
import java.time.Year;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class YearlySettlementTasklet implements Tasklet {
    private final SettlementRepository settlementRepository;
    private final SettlementValidator settlementValidator;
    private final SettlementAggregator settlementAggregator;
    private final MonthlySettlementRepository monthlySettlementRepository;
    private final SettlementSaver settlementSaver;

    @Value("#{jobParameters['cutoff']}")
    private String cutoffString;

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {
        // 1. 연도 계산
        LocalDate cutoffDate = LocalDateTime.parse(cutoffString).toLocalDate();
        Year year = Year.from(cutoffDate);
        // 2. 정산 대상 사용자 목록 조회
        List<Long> userIds = settlementRepository.findDistinctUserIds();
        for (Long userId : userIds) {
            // 3. 월별 정산 데이터 조회
            List<MonthlySettlement> settlements = monthlySettlementRepository.findByMonthlySettlement(userId);
            log.info("[연별 집계] userId= {} 조회된 정산 건수= {}", userId, settlements.size());
            // 4. 검증
            if (settlements.isEmpty()) {
                log.info("[연별 집계] userId= {} {}년 데이터 없음", userId, year);
            }
            settlementValidator.validateYearly(settlements);
            // 5. 연 기준으로 그룹핑 + 합산
            Map<Short, SettlementTotals> yearlyTotalsMap =
                    settlementAggregator.aggregate(
                            settlements,
                            m -> m.getYear(),   // grouping key
                            m -> new SettlementTotals(
                                    m.getTotalSales(),
                                    m.getTotalFee(),
                                    m.getTotalVat(),
                                    m.getTotalRefund(),
                                    m.getTotalSettlement()
                            )
                    );
            // 6. upsert(저장)
            yearlyTotalsMap.forEach((y, totals) ->
                    settlementSaver.saveYearly(userId, y, totals)
            );
            log.info("[연별 집계] userId= {} {}년 정산 {}건 완료", userId, year, settlements.size());
        }
        log.info("YearlySettlementTasklet 성공");
        return RepeatStatus.FINISHED;
    }
}
