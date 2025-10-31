package com.homesweet.homesweetback.domain.settlement.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklySettlementResponse(
        Long year,
        Short month,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        BigDecimal totalSales,
        BigDecimal totalFee,
        BigDecimal totalVat,
        BigDecimal totalRefund,
        BigDecimal totalSettlement,

        Double completedRate,   // 정산 완료율
        int totalCount  // 총 주 거래 건수
) {
}
