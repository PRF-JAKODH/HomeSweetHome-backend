package com.homesweet.homesweetback.domain.settlement.util.saver;
import com.homesweet.homesweetback.domain.settlement.repository.querydsl.CustomDailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.querydsl.CustomMonthlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.querydsl.CustomWeeklySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.querydsl.CustomYearlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.vo.SettlementTotals;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

@Component
@RequiredArgsConstructor
public class SettlementSaver {
    private final CustomDailySettlementRepository customDailySettlementRepository;
    private final CustomWeeklySettlementRepository customWeeklySettlementRepository;
    private final CustomMonthlySettlementRepository customMonthlySettlementRepository;
    private final CustomYearlySettlementRepository customYearlySettlementRepository;

    // 일별
    public void saveDaily(Long userId, LocalDate date, SettlementTotals totals) {
        customDailySettlementRepository.upsertDaily(
                userId,
                date.atStartOfDay(),
                totals
        );
    }

    // 주별
    public void saveWeekly(Long userId, LocalDate weekStartDate, SettlementTotals totals) {
        if (weekStartDate == null) {
            throw new NullPointerException("weekStartDate cannot be null");
        }
        if (totals == null) {
            throw new NullPointerException("totals cannot be null");
        }
        customWeeklySettlementRepository.upsertWeekly(
                userId,
                weekStartDate,
                totals
        );
    }

    // 월별
    public void saveMonthly(Long userId, YearMonth ym, SettlementTotals totals) {
        customMonthlySettlementRepository.upsertMonthly(
                userId,
                (short) ym.getYear(),
                (byte) ym.getMonthValue(),
                totals
        );
    }

    // 연별
    public void saveYearly(Long userId, Short year, SettlementTotals totals) {

        customYearlySettlementRepository.upsertYearly(
                userId,
                year,
                totals
        );
    }
}
