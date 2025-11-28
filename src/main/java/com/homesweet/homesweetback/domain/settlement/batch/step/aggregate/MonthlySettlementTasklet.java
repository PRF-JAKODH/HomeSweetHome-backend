package com.homesweet.homesweetback.domain.settlement.batch.step.aggregate;

import com.homesweet.homesweetback.domain.settlement.aggregate.SettlementAggregator;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.WeeklySettlementRepository;
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
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class MonthlySettlementTasklet implements Tasklet {
    private final SettlementRepository settlementRepository;
    private final SettlementValidator settlementValidator;
    private final SettlementAggregator settlementAggregator;
    private final WeeklySettlementRepository weeklySettlementRepository;
    private final SettlementSaver settlementSaver;

    @Value("#{jobParameters['cutoff']}")
    private String cutoffString;

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext){
        // 1. 월 계산
        LocalDate cutoffDate = LocalDateTime.parse(cutoffString).toLocalDate();
        YearMonth yearMonth = YearMonth.from(cutoffDate);
        log.info("MonthlySettlementTasklet 시작: {}월", yearMonth);
        // 2. 정산 대상 사용자 목록 조회
        List<Long> userIds = settlementRepository.findDistinctUserIds();
        for(Long userId : userIds){
            // 3. 주별 정산 데이터 조회
            List<WeeklySettlement> settlements = weeklySettlementRepository.findByWeeklySettlement(userId);
            log.info("[월별 집계] userId= {} 조회된 정산 건수= {}", userId, settlements.size());
            // 4. 검증
            settlementValidator.validateMonthly(settlements);
            if (settlements.isEmpty()) {
                log.info("[월별 집계] userId= {} {}월 데이터 없음", userId, yearMonth);
            }
            // 5. 월 기준으로 그룹핑 + 합산
            Map<YearMonth, SettlementTotals> monthlyTotalsMap =
                    settlementAggregator.aggregate(
                            settlements,
                            w -> YearMonth.of(w.getYear(), w.getMonth()),   // 월별 그룹핑 Key
                            w -> new SettlementTotals(
                                    w.getTotalSales(),
                                    w.getTotalFee(),
                                    w.getTotalVat(),
                                    w.getTotalRefund(),
                                    w.getTotalSettlement()
                            )
                    );

            // 6. upsert(저장)
            monthlyTotalsMap.forEach((ym, settlementTotals) -> {
                settlementSaver.saveMonthly(userId, ym, settlementTotals);
            });
            log.info("[월별 집계] userId= {} {}월 정산 {}건 완료", userId, yearMonth, settlements.size());
        }
        log.info("MonthlySettlementTasklet 성공");
        return RepeatStatus.FINISHED;
    }
}
