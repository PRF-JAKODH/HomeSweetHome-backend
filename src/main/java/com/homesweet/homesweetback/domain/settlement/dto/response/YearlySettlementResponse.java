package com.homesweet.homesweetback.domain.settlement.dto.response;

import java.math.BigDecimal;

public record YearlySettlementResponse(
        Short year,
        BigDecimal totalSales,
        BigDecimal totalFee,
        BigDecimal totalVat,
        BigDecimal totalRefund,
        BigDecimal totalSettlement,

        Long totalCount  // 총 거래 건수
) {
}