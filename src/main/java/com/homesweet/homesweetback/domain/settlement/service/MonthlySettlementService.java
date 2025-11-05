package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.domain.settlement.dto.response.MonthlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.dto.response.YearlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.MonthlySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.MonthlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.WeeklySettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthlySettlementService {
    private final MonthlySettlementRepository monthlySettlementRepository;
    private final WeeklySettlementRepository weeklySettlementRepository;
    private final SettlementRepository settlementRepository;

    public MonthlySettlementResponse getMonthlySummary(Long userId, LocalDate date) {
        List<WeeklySettlement> settlements = weeklySettlementRepository.findByWeeklySettlement(userId);
        System.out.println("settlements: " + settlements);
        YearMonth currentMonth = YearMonth.from(date);
        YearMonth lastMonth = currentMonth.minusMonths(1);
        BigDecimal currentTotal = BigDecimal.ZERO;
        BigDecimal lastTotal = BigDecimal.ZERO;

        Short currYear = (short) currentMonth.getYear();
        Byte currMonth = (byte) currentMonth.getMonthValue();


        Short lastYear = (short) lastMonth.getYear();
        Byte lastMonthValue = (byte) lastMonth.getMonthValue();

        BigDecimal currentTotalSales = BigDecimal.ZERO;
        BigDecimal currentTotalFee = BigDecimal.ZERO;
        BigDecimal currentTotalVat = BigDecimal.ZERO;
        BigDecimal currentTotalRefund = BigDecimal.ZERO;
        BigDecimal currentTotalSettlement = BigDecimal.ZERO;
        int currentCount = settlements.size();

        for (WeeklySettlement m : settlements) {
            if (m.getYear().equals(currYear) && m.getMonth().equals(currMonth)) {
                currentTotalSettlement = currentTotalSettlement.add(m.getTotalSettlement());
                currentTotal = currentTotal.add(m.getTotalSettlement());
                currentTotalSales = currentTotalSales.add(m.getTotalSales());
                currentTotalFee = currentTotalFee.add(m.getTotalFee());
                currentTotalVat = currentTotalVat.add(m.getTotalVat());
                currentTotalRefund = currentTotalRefund.add(m.getTotalRefund());

                }
            if (m.getYear().equals(lastYear) && m.getMonth().equals(lastMonthValue)) {
                lastTotal = lastTotal.add(m.getTotalSettlement());
            }
        }
        BigDecimal growthRate = BigDecimal.ZERO;
        if (lastTotal.compareTo(BigDecimal.ZERO) == 0) {
            growthRate = BigDecimal.valueOf(currentTotal.compareTo(BigDecimal.ZERO));
        } else {
            growthRate = BigDecimal.valueOf(currentTotal.subtract(lastTotal)
                    .divide(lastTotal, 1, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue());
        }
        return new MonthlySettlementResponse(
                currYear,
                currMonth,
                currentTotalSales,
                currentTotalFee,
                currentTotalVat,
                currentTotalRefund,
                currentTotalSettlement,
                growthRate.doubleValue(),
                currentCount
        );
    }

    // 월 집계
    public void getMonthlySettlement(Long userId) {
        Short prevYear = null;
        Byte prevMonth = null;
        // 월 합계
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalSettlement = BigDecimal.ZERO;

        List<WeeklySettlement> settlements = weeklySettlementRepository.findByWeeklySettlement(userId);
        if (settlements == null || settlements.isEmpty()) {
            System.out.println("조회된 정산 데이터가 없어요");
        }
        for (WeeklySettlement w : settlements) {
            Short year = w.getYear();
            Byte month = w.getMonth();
            // 첫번째 데이터 초기화
            if(prevYear == null){
                prevYear = year;
                prevMonth = month;
            }
            // 시작 월이 변경되면 upsert
            if (!year.equals(prevYear) || !month.equals(prevMonth)) {
                monthlySettlementRepository.upsertMonthly(
                        userId,
                        prevYear,
                        prevMonth,
                        totalSales,
                        totalFee,
                        totalVat,
                        totalRefund,
                        totalSettlement
                );
                // 다음 월
                prevYear = year;
                prevMonth = month;
                System.out.println(prevYear + "/" + prevMonth);
                totalSales = BigDecimal.ZERO;
                totalFee = BigDecimal.ZERO;
                totalVat = BigDecimal.ZERO;
                totalRefund = BigDecimal.ZERO;
                totalSettlement = BigDecimal.ZERO;
            }
            // 현재 월의 누적
            totalSales = totalSales.add(w.getTotalSales());
            totalFee = totalFee.add(w.getTotalFee());
            totalVat = totalVat.add(w.getTotalVat());
            totalRefund = totalRefund.add(w.getTotalRefund());
            totalSettlement = totalSettlement.add(w.getTotalSettlement());

        }
        // 마지막 월 upsert
        if (prevMonth != null){
            monthlySettlementRepository.upsertMonthly(
                    userId,
                    prevYear,
                    prevMonth,
                    totalSales,
                    totalFee,
                    totalVat,
                    totalRefund,
                    totalSettlement
            );
        }
    }
}