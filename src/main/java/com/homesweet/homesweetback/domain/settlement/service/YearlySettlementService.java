package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.domain.settlement.aggregate.SettlementAggregator;
import com.homesweet.homesweetback.domain.settlement.dto.response.YearlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.MonthlySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.YearlySettlement;
import com.homesweet.homesweetback.domain.settlement.mapper.SettlementMapper;
import com.homesweet.homesweetback.domain.settlement.repository.MonthlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.YearlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.dto.response.EmptyResponse;
import com.homesweet.homesweetback.domain.settlement.util.calculator.YearlyDateRangeCalculator;
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
public class YearlySettlementService {
    private final YearlySettlementRepository yearlySettlementRepository;
    private final YearlyDateRangeCalculator yearlyCalc;
    private final EmptyResponse emptyResponse;
    private final SettlementCacheService settlementCacheService;

    private final MonthlySettlementRepository monthlySettlementRepository;
    private final SettlementRepository settlementRepository;
    private final YearlyDateRangeCalculator yearlyDateRangeCalculator;
    private final SettlementMapper settlementMapper;
    private final SettlementValidator settlementValidator;
    private final SettlementAggregator settlementAggregator;
    private final SettlementSaver settlementSaver;

    // 연별 데이터 조회
    @Transactional(readOnly = true)
    public Page<YearlySettlementResponse> getYearlySummary(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // 1) 캐시에서 content 가져오기
        List<YearlySettlementResponse> content =
                settlementCacheService.getYearlyContentCache(userId, startDate, endDate, pageable);

        if (content.isEmpty()) {
            int year = startDate.getYear();
            return emptyResponse.createEmptyYearly((short) year, pageable);
        }

        // 2) count 조회
        YearlyDateRangeCalculator.YearlyDateRange range =
                yearlyCalc.calculate(startDate, endDate);

        long totalCount = yearlySettlementRepository.countByRange(
                userId,
                range.fromYear(),
                range.toYearExclusive()
        );

        // 3) Page 반환
        return new PageImpl<>(content, pageable, totalCount);
    }
    // 연별 집계
    public void getYearlySettlement(Long userId) {
        Short prevYear = null;
        List<MonthlySettlement> settlements = monthlySettlementRepository.findByMonthlySettlement(userId);
        // 2. 검증
        settlementValidator.validateYearly(settlements);
        // 3. 공통 연별 집계 처리
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
        // 4. upsert
        yearlyTotalsMap.forEach((year, totals) ->
                settlementSaver.saveYearly(userId, year, totals)
        );
    }
}