package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.domain.settlement.aggregate.SettlementAggregator;
import com.homesweet.homesweetback.domain.settlement.dto.response.CachedResult;
import com.homesweet.homesweetback.domain.settlement.dto.response.CachedResultWithStats;
import com.homesweet.homesweetback.domain.settlement.dto.response.WeeklySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.YearlySettlement;
import com.homesweet.homesweetback.domain.settlement.mapper.SettlementMapper;
import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.WeeklySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.dto.response.EmptyResponse;
import com.homesweet.homesweetback.domain.settlement.repository.YearlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.calculator.SettlementCalculator;
import com.homesweet.homesweetback.domain.settlement.util.calculator.WeeklyDateRangeCalculator;
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
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklySettlementService {
    private final WeeklySettlementRepository weeklySettlementRepository;
    private final EmptyResponse emptyResponse;
    private final SettlementCacheService settlementCacheService;
    private final WeeklyDateRangeCalculator weeklyCalc;
    private final DailySettlementRepository dailySettlementRepository;
    private final SettlementCalculator settlementCalculator;
    private final SettlementMapper settlementMapper;
    private final SettlementValidator settlementValidator;
    private final SettlementAggregator settlementAggregator;
    private final SettlementSaver settlementSaver;
    private final YearlySettlementRepository yearlySettlementRepository;

    // 주별 데이터 조회(페이지 처리)
    @Transactional(readOnly = true)
    public Page<WeeklySettlementResponse> getWeeklySummary(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // 1) 캐시에서 content 가져오기
        List<WeeklySettlementResponse> content =
                settlementCacheService.getWeeklyContentCache(userId, startDate, endDate, pageable);


        if (content.isEmpty()) {
            WeeklyDateRangeCalculator.WeeklyDateRange range =
                    weeklyCalc.getWeeklyDateRange(startDate, endDate);
            return emptyResponse.createEmptyWeekly(range, pageable);
        }

        // 2) count 조회
        WeeklyDateRangeCalculator.WeeklyDateRange range =
                weeklyCalc.getWeeklyDateRange(startDate, endDate);

        long totalCount = weeklySettlementRepository.countByRange(
                userId,
                range.firstWeekStart(),
                range.lastWeekStartEx()
        );

        // 3) PageImpl 생성 후 반환
        return new PageImpl<>(content, pageable, totalCount);
    }
    // 주차별 정산내역
    public void getWeeklySettlement(Long userId, LocalDate weekStart, LocalDate weekEnd) {
        // 1. 일별 집계내역 조회
        List<DailySettlement> settlements = findDailySettlements(userId);

        // 2. 검증
        settlementValidator.validateWeekly(settlements);

        // 3. 공통 집계 처리
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
        // 4. upsert(저장)
        weeklyTotalsMap.forEach((weekStartDate, totals) ->
                settlementSaver.saveWeekly(userId, weekStartDate, totals)
        );
    }
    private Page<WeeklySettlement> findWeeklySettlements(Long userId, Pageable pageable, LocalDate firstWeekStart, LocalDate lastWeekStartEx) {
        Page<WeeklySettlement> weeklySettlements = weeklySettlementRepository.findByWeeklySettlementByRange(userId, firstWeekStart, lastWeekStartEx, pageable);
        return weeklySettlements;
    }
    private List<DailySettlement> findDailySettlements(Long userId) {
        List<DailySettlement> settlements = dailySettlementRepository.findByDailySettlement(userId);
        return settlements;
    }

}