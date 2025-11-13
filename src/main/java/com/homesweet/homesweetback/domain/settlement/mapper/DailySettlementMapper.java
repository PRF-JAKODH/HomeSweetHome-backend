package com.homesweet.homesweetback.domain.settlement.mapper;

import com.homesweet.homesweetback.domain.settlement.dto.response.DailySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class DailySettlementMapper {
//    private static void extracted(Page<DailySettlement> dailySettlements, List<DailySettlementResponse> dailySettlement) {
//    for (DailySettlement d : dailySettlements.getContent()) { // 페이지의 실제 리스트
//        LocalDate settlementDate = d.getSettlementDate().toLocalDate(); // 정산일시
//
//        // 기본은 PENDING
//        String settlementStatus = (completedCount == totalCount) ? "COMPLETED" : "PENDING";
//        dailySettlement.add(new DailySettlementResponse(
//                d.getTotalSales(),
//                d.getTotalFee(),
//                d.getTotalVat(),
//                d.getTotalRefund(),
//                d.getTotalSettlement(),
//                settlementDate,
//                settlementStatus,
//                completedRate,
//                totalCount
//        ));
//    }

    public DailySettlementResponse toDailySettlementResponse(DailySettlement d, long totalCount, long completedCount, double completedRate) {
        LocalDate settlementDate = d.getSettlementDate().toLocalDate(); // 정산일시
        // 기본은 PENDING
        String settlementStatus = (completedCount == totalCount) ? "COMPLETED" : "PENDING";
        return new DailySettlementResponse(
                d.getTotalSales(),
                d.getTotalFee(),
                d.getTotalVat(),
                d.getTotalRefund(),
                d.getTotalSettlement(),
                settlementDate,
                settlementStatus,
                completedRate,
                totalCount
        );
    }
    public List<DailySettlementResponse> toDailySettlementResponseList(List<DailySettlement> dailySettlement, long totalCount, long completedCount, double completedRate) {
        List<DailySettlementResponse> dailySettlementResponseList = new ArrayList<>(dailySettlement.size());
        for (DailySettlement d : dailySettlement){
            dailySettlementResponseList.add(toDailySettlementResponse(d, totalCount, completedCount, completedRate));
        }
        return dailySettlementResponseList;
    }
}
