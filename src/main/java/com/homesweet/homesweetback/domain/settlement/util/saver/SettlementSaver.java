package com.homesweet.homesweetback.domain.settlement.util.saver;

import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.WeeklySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.vo.SettlementTotals;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class SettlementSaver {
    private final DailySettlementRepository dailySettlementRepository;
    private final WeeklySettlementRepository weeklySettlementRepository;

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
}
