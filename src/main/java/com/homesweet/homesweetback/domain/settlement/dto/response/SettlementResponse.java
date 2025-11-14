package com.homesweet.homesweetback.domain.settlement.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementResponse(
        LocalDateTime orderedAt,
        String orderNumber,
        String productName,
        BigDecimal salesAmount,
        BigDecimal fee,
        BigDecimal vat,
        BigDecimal refundAmount,
        BigDecimal settlementAmount,
        LocalDateTime settlementDate,
        String settlementStatus
) {
}