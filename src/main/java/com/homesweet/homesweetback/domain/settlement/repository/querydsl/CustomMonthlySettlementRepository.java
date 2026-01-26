package com.homesweet.homesweetback.domain.settlement.repository.querydsl;

import com.homesweet.homesweetback.domain.settlement.util.vo.SettlementTotals;

public interface CustomMonthlySettlementRepository {
    int upsertMonthly(Long userId, Short year, Byte month, SettlementTotals totals);
}
