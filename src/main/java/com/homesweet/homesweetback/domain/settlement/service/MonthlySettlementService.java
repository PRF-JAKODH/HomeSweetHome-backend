package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.domain.settlement.aggregate.SettlementAggregator;
import com.homesweet.homesweetback.domain.settlement.dto.response.MonthlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.mapper.SettlementMapper;
import com.homesweet.homesweetback.domain.settlement.repository.MonthlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.WeeklySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.dto.response.EmptyResponse;
import com.homesweet.homesweetback.domain.settlement.util.calculator.MonthlyDateRangeCalculator;
import com.homesweet.homesweetback.domain.settlement.util.saver.SettlementSaver;
import com.homesweet.homesweetback.domain.settlement.util.vo.SettlementTotals;
import com.homesweet.homesweetback.domain.settlement.validation.SettlementValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlySettlementService {
    private final MonthlySettlementRepository monthlySettlementRepository;
    private final WeeklySettlementRepository weeklySettlementRepository;
    private final MonthlyDateRangeCalculator monthlyCalc;
    private final EmptyResponse emptyResponse;
    private final SettlementCacheService settlementCacheService;
    private final SettlementRepository settlementRepository;
    private final MonthlyDateRangeCalculator monthlyDateRangeCalculator;
    private final SettlementMapper settlementMapper;
    private final SettlementValidator settlementValidator;
    private final SettlementAggregator settlementAggregator;
    private final SettlementSaver settlementSaver;

    // 월별 데이터 조회(페이지 처리)
    @Transactional(readOnly = true)
    public Page<MonthlySettlementResponse> getMonthlySummary(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        long t1 = System.currentTimeMillis();

        // 1) 캐시에서 content 가져오기
        List<MonthlySettlementResponse> content =
                settlementCacheService.getMonthlyContentCache(userId, startDate, endDate, pageable);

        long t2 = System.currentTimeMillis();

        if (content.isEmpty()) {
            YearMonth ym = YearMonth.from(startDate);
            return emptyResponse.createEmptyMonthly(ym, pageable);
        }

        // 2) count 조회
        MonthlyDateRangeCalculator.MonthlyDateRange range =
                monthlyCalc.MonthlyDateRangeCalculate(startDate, endDate);

        long totalCount = monthlySettlementRepository.countByRange(
                userId,
                range.fromYear(),
                range.fromMonth(),
                range.toYear(),
                range.toMonth()
        );

        long t3 = System.currentTimeMillis();

        log.info("[PERF][MONTHLY] CACHE={}ms, COUNT={}ms", (t2 - t1), (t3 - t2));

        // 3) Page 반환
        return new PageImpl<>(content, pageable, totalCount);
    }
    public void getMonthlySettlement(Long userId) {
        // 1. 주별 집계내역 조회
        List<WeeklySettlement> settlements = findWeeklySettlements(userId);
        // 2. 검증
        settlementValidator.validateMonthly(settlements);
        // 3. 공통 월별 집계 처리
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
        // 4. upsert
        monthlyTotalsMap.forEach((yearMonth, totals) ->
                settlementSaver.saveMonthly(userId, yearMonth, totals)
        );
    }

    private List<WeeklySettlement> findWeeklySettlements(Long userId) {
        return weeklySettlementRepository.findByWeeklySettlement(userId);
    }
}