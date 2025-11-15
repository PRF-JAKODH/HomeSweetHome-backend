package com.homesweet.homesweetback.domain.settlement.mapper;

import com.homesweet.homesweetback.domain.settlement.dto.response.DailySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.dto.response.WeeklySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.util.calculator.SettlementCalculator;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

// 기간별 응답 매핑
@Component
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

    // 일별 리스트 매핑
    public List<DailySettlementResponse> toDailySettlementResponseList(List<DailySettlement> dailySettlement, Function<DailySettlement, SettlementCalculator.SettlementStats> stats) {
//        List<DailySettlementResponse> dailySettlementResponseList = new ArrayList<>(dailySettlement.size());
        return dailySettlement.stream().map(d-> toDailySettlementResponse(d, stats.apply(d))).toList();
    }

    // 주별 매핑
    public List<WeeklySettlementResponse> toWeeklySettlementResponse(List<WeeklySettlement> ws, SettlementCalculator.SettlementStats stats, byte week) {
        return ws.stream().map(w-> new WeeklySettlementResponse(
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
}
