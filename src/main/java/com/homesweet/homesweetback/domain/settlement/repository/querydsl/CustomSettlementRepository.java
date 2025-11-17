package com.homesweet.homesweetback.domain.settlement.repository.querydsl;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;


public interface CustomSettlementRepository {
    int applyRefundAmount(Long orderId, BigDecimal refundAmount);

}
