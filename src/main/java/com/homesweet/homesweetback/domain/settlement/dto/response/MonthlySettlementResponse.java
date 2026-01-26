package com.homesweet.homesweetback.domain.settlement.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlySettlementResponse(
        Short year,
        Byte month,
        BigDecimal totalSales,
        BigDecimal totalFee,
        BigDecimal totalVat,
        BigDecimal totalRefund,
        BigDecimal totalSettlement,

        Double growthRate,   // 전월대비 증감율
        Long totalCount  // 총 거래 건수
) {
}
