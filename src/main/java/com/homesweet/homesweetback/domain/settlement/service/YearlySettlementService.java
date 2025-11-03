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
        System.out.println("totalCount = " + totalCount);

        for (MonthlySettlement m : settlements) {
            totalSales = totalSales.add(m.getTotalSales());
            totalFee = totalFee.add(m.getTotalFee());
            totalVat = totalVat.add(m.getTotalVat());
            totalRefund = totalRefund.add(m.getTotalRefund());
            totalSettlement = totalSettlement.add(m.getTotalSettlement());
            totalCount++;
        }
        System.out.println("totalSales: " + totalSales);

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

    public void getYearlySettlement(Long userId) {
        List<MonthlySettlement> settlements = monthlySettlementRepository.findByMonthlySettlement(userId);
        System.out.println("settlements: " + settlements);
        if (settlements == null || settlements.isEmpty()) {
            System.out.println("조회된 정산 데이터가 없어요");
        }
        Short prevYear = null;
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalSettlement = BigDecimal.ZERO;

        for (MonthlySettlement y : settlements) {
            Short year = y.getYear();
            if (prevYear != null && !prevYear.equals(year)) {
                yearlySettlementRepository.save(
                        YearlySettlement.builder()
                                .userId(y.getUserId())
                                .year(prevYear)
                                .totalSales(totalSales)
                                .totalFee(totalFee)
                                .totalVat(totalVat)
                                .totalRefund(totalRefund)
                                .totalSettlement(totalSettlement)
                                .build()
                );
                totalSales = BigDecimal.ZERO;
                totalFee = BigDecimal.ZERO;
                totalVat = BigDecimal.ZERO;
                totalRefund = BigDecimal.ZERO;
                totalSettlement = BigDecimal.ZERO;
            }
            totalSales = totalSales.add(y.getTotalSales());
            totalFee = totalFee.add(y.getTotalFee());
            totalVat = totalVat.add(y.getTotalVat());
            totalRefund = totalRefund.add(y.getTotalRefund());
            totalSettlement = totalSettlement.add(y.getTotalSettlement());
            prevYear = year;
        }
        YearlySettlement yearlySettlement = YearlySettlement.builder()
                .userId(userId)
                .year(prevYear)
                .totalSales(totalSales)
                .totalFee(totalFee)
                .totalVat(totalVat)
                .totalRefund(totalRefund)
                .totalSettlement(totalSettlement)
                .build();
        yearlySettlementRepository.save(yearlySettlement);
    }
}