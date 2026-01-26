package com.homesweet.homesweetback.domain.settlement.repository.querydsl;

import com.homesweet.homesweetback.domain.settlement.util.vo.SettlementTotals;

import java.time.LocalDateTime;

public interface CustomDailySettlementRepository {
    void upsertDaily(Long userId, LocalDateTime settlementDate, SettlementTotals totals);
}
