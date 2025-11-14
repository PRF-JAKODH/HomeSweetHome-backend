package com.homesweet.homesweetback.domain.settlement.util.saver;

import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.vo.DailyTotals;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DailySettlementSaver {
    private final DailySettlementRepository dailySettlementRepository;

    public void saveDaily(Long userId, LocalDate date, DailyTotals totals){
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
}
