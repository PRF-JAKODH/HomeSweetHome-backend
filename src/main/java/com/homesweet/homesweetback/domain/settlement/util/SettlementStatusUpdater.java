package com.homesweet.homesweetback.domain.settlement.util;

import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SettlementStatusUpdater {
    private final SettlementRepository settlementRepository;

    public void markDailyCompleted(Long userId, LocalDateTime dailyStartDate, LocalDateTime dailyEndDate) {
        settlementRepository.markCompletedInRange(userId, dailyStartDate, dailyEndDate);
    }
}
