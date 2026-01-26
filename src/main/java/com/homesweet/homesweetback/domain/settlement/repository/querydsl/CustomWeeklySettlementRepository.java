package com.homesweet.homesweetback.domain.settlement.repository.querydsl;

import com.homesweet.homesweetback.domain.settlement.util.vo.SettlementTotals;

import java.time.LocalDate;

public interface CustomWeeklySettlementRepository {
    int upsertWeekly(Long userId, LocalDate WeekStartDate, SettlementTotals totals);
}
