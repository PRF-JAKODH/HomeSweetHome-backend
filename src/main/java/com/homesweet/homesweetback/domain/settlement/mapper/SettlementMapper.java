package com.homesweet.homesweetback.domain.settlement.mapper;

import com.homesweet.homesweetback.domain.settlement.dto.response.DailySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.dto.response.MonthlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.dto.response.WeeklySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.dto.response.YearlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.MonthlySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.YearlySettlement;
import com.homesweet.homesweetback.domain.settlement.util.calculator.MonthlyGrowthCalculator;
import com.homesweet.homesweetback.domain.settlement.util.calculator.SettlementCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

// 기간별 응답 매핑
@Component
@RequiredArgsConstructor
public class SettlementMapper {
    // 일별 응답 매핑
    public DailySettlementResponse toDailySettlementResponse(DailySettlement d, SettlementCalculator.SettlementStats stats) {
        LocalDate settlementDate = d.getSettlementDate().toLocalDate(); // 정산일시

        // 기본은 PENDING
        String settlementStatus = (stats.completedCount() == stats.totalCount()) ? "COMPLETED" : "PENDING";
        return new DailySettlementResponse(
                d.getTotalSales(),
                d.getTotalFee(),
                d.getTotalVat(),
                d.getTotalRefund(),
                d.getTotalSettlement(),
                settlementDate,
                settlementStatus,
                stats.completedRate(),
                stats.totalCount()
        );
    }

    // 일별 리스트 매핑 stream을 사용하지 않는게 더 좋음!
    public List<DailySettlementResponse> toDailySettlementResponseList(List<DailySettlement> dailySettlement, SettlementCalculator.SettlementStats stats) {
        int size = dailySettlement.size();
        List<DailySettlementResponse> responses = new ArrayList<>(size);
        for(DailySettlement d : dailySettlement) {
            responses.add(toDailySettlementResponse(d, stats));
        }
        return responses;
    }

    // 주별 매핑
    public List<WeeklySettlementResponse> toWeeklySettlementResponse(List<WeeklySettlement> ws, SettlementCalculator.SettlementStats stats, byte week) {
        return ws.stream().map(w -> new WeeklySettlementResponse(
                w.getYear(),
                w.getMonth(),
                week,
                w.getWeekStartDate(),
                w.getWeekEndDate(),
                w.getTotalSales(),
                w.getTotalFee(),
                w.getTotalVat(),
                w.getTotalRefund(),
                w.getTotalSettlement(),
                stats.completedRate(),
                stats.totalCount()
        )).toList();
    }

    private final MonthlyGrowthCalculator monthlyGrowthCalculator;

    // 월별 리스트 매핑
    public List<MonthlySettlementResponse> toMonthlyResponses(List<MonthlySettlement> list, long totalCount) {
        List<MonthlySettlementResponse> responses = new ArrayList<>();
        BigDecimal prevTotal = null;

        for (MonthlySettlement m : list) {
            BigDecimal curr = m.getTotalSales();
            double growth = monthlyGrowthCalculator.growthCalculate(prevTotal, curr);

            responses.add(new MonthlySettlementResponse(
                    m.getYear(),
                    m.getMonth(),
                    m.getTotalSales(),
                    m.getTotalFee(),
                    m.getTotalVat(),
                    m.getTotalRefund(),
                    m.getTotalSettlement(),
                    growth,
                    totalCount
            ));
            prevTotal = curr;
        }
        return responses;
    }
    // 연별 리스트 매핑
    public List<YearlySettlementResponse> toYearlyResponses(List<YearlySettlement> list, long totalCount) {
        return list.stream()
                .map(y -> new YearlySettlementResponse(
                        y.getYear(),
                        y.getTotalSales(),
                        y.getTotalFee(),
                        y.getTotalVat(),
                        y.getTotalRefund(),
                        y.getTotalSettlement(),
                        totalCount
                ))
                .toList();
    }
}