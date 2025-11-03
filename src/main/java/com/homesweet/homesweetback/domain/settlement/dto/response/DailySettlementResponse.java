package com.homesweet.homesweetback.domain.settlement.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySettlementResponse(
        LocalDate orderedAt,
        BigDecimal totalSales,
        BigDecimal totalFee,
        BigDecimal totalVat,
        BigDecimal totalRefund,
        BigDecimal totalSettlement,
        LocalDate settlementDate,
        String settlementStatus,

        Double completedRate,   // 정산 완료율
        int totalCount,  // 총 거래 건수
        boolean empty
) {
}