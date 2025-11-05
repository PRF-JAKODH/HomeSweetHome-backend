package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.domain.settlement.dto.response.YearlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.MonthlySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.YearlySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.MonthlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.YearlySettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class YearlySettlementService {
    private final YearlySettlementRepository yearlySettlementRepository;
    private final MonthlySettlementRepository monthlySettlementRepository;
    private final SettlementRepository settlementRepository;

    public YearlySettlementResponse getYearlySummary(Long userId, LocalDate date) {
        List<MonthlySettlement> settlements = monthlySettlementRepository.findByMonthlySettlement(userId);
        System.out.println("settlements: " + settlements);
        short year = (short) date.getYear();
        LocalDate firstDayOfYear = date.withDayOfYear(1);  // 올해 1월 1일
        LocalDate lastDayOfYear  = firstDayOfYear.withMonth(12).withDayOfMonth(31);  // 올해 12월 31일
        System.out.println("firstDayOfYear = " + firstDayOfYear);
//        LocalDateTime startDate = firstDayOfYear.atStartOfDay();
//        LocalDateTime endDate   = lastDayOfYear.atTime(23, 59, 59);

//        List<Settlement> settlements = settlementRepository
//                .findByUserIdAndOrderedAtBetween(userId, startDate, endDate);
        if (settlements.isEmpty()) {
            return new YearlySettlementResponse(
                    (long) firstDayOfYear.getYear(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0
            );
        }

        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalSettlement = BigDecimal.ZERO;
        int totalCount = settlements.size();

        for (MonthlySettlement m : settlements) {
            totalSales = totalSales.add(m.getTotalSales());
            totalFee = totalFee.add(m.getTotalFee());
            totalVat = totalVat.add(m.getTotalVat());
            totalRefund = totalRefund.add(m.getTotalRefund());
            totalSettlement = totalSettlement.add(m.getTotalSettlement());
            totalCount++;
        }

        return new YearlySettlementResponse(
                (long) firstDayOfYear.getYear(),
                totalSales,
                totalFee,
                totalVat,
                totalRefund,
                totalSettlement,
                totalCount
        );
    }

    // 연 집계
    public void getYearlySettlement(Long userId) {
        Short prevYear = null;
        List<MonthlySettlement> settlements = monthlySettlementRepository.findByMonthlySettlement(userId);
        if (settlements == null || settlements.isEmpty()) {
            System.out.println("조회된 정산 데이터가 없어요");
        }
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalSettlement = BigDecimal.ZERO;

        for (MonthlySettlement y : settlements) {
            Short year = y.getYear();
            if (prevYear == null) {
                prevYear = year;    // 연도 초기화
            }
            // 연도가 변경되면 upsert
            if (!year.equals(prevYear)) {
                yearlySettlementRepository.upsertYearly(
                        userId,
                        prevYear,
                        totalSales,
                        totalFee,
                        totalVat,
                        totalRefund,
                        totalSettlement
                );
                // 다음 연도
                prevYear = year;
                totalSales = BigDecimal.ZERO;
                totalFee = BigDecimal.ZERO;
                totalVat = BigDecimal.ZERO;
                totalRefund = BigDecimal.ZERO;
                totalSettlement = BigDecimal.ZERO;
            }
            // 현재 연도의 누적
            totalSales = totalSales.add(y.getTotalSales());
            totalFee = totalFee.add(y.getTotalFee());
            totalVat = totalVat.add(y.getTotalVat());
            totalRefund = totalRefund.add(y.getTotalRefund());
            totalSettlement = totalSettlement.add(y.getTotalSettlement());
        }
        // 마지막 연도
        if(prevYear != null) {
            yearlySettlementRepository.upsertYearly(
                    userId,
                    prevYear,
                    totalSales,
                    totalFee,
                    totalVat,
                    totalRefund,
                    totalSettlement
            );
        }
    }
}