package com.homesweet.homesweetback.domain.settlement.util.saver;

import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.MonthlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.WeeklySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.YearlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.vo.SettlementTotals;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;

@Component
@RequiredArgsConstructor
public class SettlementSaver {
    private final DailySettlementRepository dailySettlementRepository;
    private final WeeklySettlementRepository weeklySettlementRepository;
    private final MonthlySettlementRepository monthlySettlementRepository;
    private final YearlySettlementRepository yearlySettlementRepository;
    // 일별
    public void saveDaily(Long userId, LocalDate date, SettlementTotals totals){
        dailySettlementRepository.upsertDaily(
                userId,
                date.atStartOfDay(),
                totals.getTotalSales(),
                totals.getTotalFee(),
                totals.getTotalVat(),
                totals.getTotalRefund(),
                totals.getTotalSettlement()
        );
    }
    // 주별
    public void saveWeekly(Long userId, LocalDate weekStartDate, SettlementTotals totals){
        LocalDate weekEndDate = weekStartDate.plusDays(6);
        weeklySettlementRepository.upsertWeekly(
                    userId,
                    (short) weekStartDate.getYear(),
                    (byte) weekStartDate.getMonthValue(),
                    weekStartDate,
                    weekEndDate,
                    totals.getTotalSales(),
                    totals.getTotalFee(),
                    totals.getTotalVat(),
                    totals.getTotalRefund(),
                    totals.getTotalSettlement()
            );
    }
    // 월별
    public void saveMonthly(Long userId, YearMonth ym, SettlementTotals totals){
        monthlySettlementRepository.upsertMonthly(
                userId,
                (short) ym.getYear(),
                (byte) ym.getMonthValue(),
                totals.getTotalSales(),
                totals.getTotalFee(),
                totals.getTotalVat(),
                totals.getTotalRefund(),
                totals.getTotalSettlement()
        );
    }
    // 연별
    public void saveYearly(Long userId, Short year, SettlementTotals totals){
        yearlySettlementRepository.upsertYearly(
                userId,
                year,
                totals.getTotalSales(),
                totals.getTotalFee(),
                totals.getTotalVat(),
                totals.getTotalRefund(),
                totals.getTotalSettlement()
        );
    }
}
