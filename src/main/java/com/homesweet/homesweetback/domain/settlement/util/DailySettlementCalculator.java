package com.homesweet.homesweetback.domain.settlement.util;

import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Component
@RequiredArgsConstructor
public class DailySettlementCalculator {
    private final SettlementRepository settlementRepository;

    /**
     * 특정 기간의 정산 통계를 계산
     *
     * @param userId 판매자 ID
     * @param start  시작일시
     * @param end    종료일시
     * @return SettlementStats (총 주문 수, 완료 건수, 완료율)
     */
    public SettlementStats calculateStats(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        long totalCount = settlementRepository.countAllByOrderedAt(userId, start, end);  // 총 주문건수
        long completedCount = settlementRepository.countCompletedSettlements(userId, start, end);
        double completedRate = totalCount == 0 ? 0.0 : Math.round(((double) completedCount * 100.0 / totalCount) * 10) / 10.0;  // 정산 완료율
        return new SettlementStats(totalCount, completedCount, completedRate);
    }

    public record SettlementStats(long totalCount, long completedCount, double completedRate) {
    }
}
