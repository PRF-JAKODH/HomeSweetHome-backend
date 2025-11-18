package com.homesweet.homesweetback.domain.settlement.repository.querydsl;

import java.math.BigDecimal;

public interface CustomSettlementRepository {
    int applyRefundAmount(Long orderId, BigDecimal refundAmount);
}
