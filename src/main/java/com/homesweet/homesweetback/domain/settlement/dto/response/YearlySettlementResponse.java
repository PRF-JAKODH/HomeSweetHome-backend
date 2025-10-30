package com.homesweet.homesweetback.domain.settlement.dto.response;

import java.math.BigDecimal;

public record YearlySettlementResponse(
        Long year,
        BigDecimal totalSales,
        BigDecimal totalFee,
        BigDecimal totalVat,
        BigDecimal totalRefund,
        BigDecimal totalSettlement,

        int totalCount  // 총 거래 건수
) {
}