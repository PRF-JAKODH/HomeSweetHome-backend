package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.domain.settlement.dto.response.MonthlySettlementResponse;
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
        List<MonthlySettlement> settlements = monthlySettlementRepository.findByMonthlySettlement(userId);
        System.out.println("settlements: " + settlements);
        YearMonth currentMonth = YearMonth.from(date);
        YearMonth lastMonth = currentMonth.minusMonths(1);
        BigDecimal currentTotal = BigDecimal.ZERO;
        BigDecimal lastTotal    = BigDecimal.ZERO;

        Short currYear = (short) currentMonth.getYear();
        Byte currMonth = (byte) currentMonth.getMonthValue();
        System.out.println("요청 기준 연/월 = " + currYear + "/" + currMonth);
        for (MonthlySettlement m : settlements) {
            System.out.println("DB 저장된 연/월 = " + m.getYear() + "/" + m.getMonth());
        }

        Short lastYear = (short) lastMonth.getYear();
        Byte lastMonthValue = (byte) lastMonth.getMonthValue();

        BigDecimal currentTotalSales = BigDecimal.ZERO;
        BigDecimal currentTotalFee = BigDecimal.ZERO;
        BigDecimal currentTotalRefund = BigDecimal.ZERO;
        BigDecimal currentTotalSettlement = BigDecimal.ZERO;
        int currentCount = 0;

        for (MonthlySettlement m : settlements) {
            if (m.getYear().equals(currYear) && m.getMonth().equals(currMonth)) {
                currentTotalSettlement = currentTotalSettlement.add(m.getTotalSettlement());
                currentTotal = currentTotal.add(m.getTotalSettlement());
                currentTotalSales = currentTotalSales.add(m.getTotalSales());
                currentTotalFee = currentTotalFee.add(m.getTotalFee());
                currentTotalRefund = currentTotalRefund.add(m.getTotalRefund());
                currentCount++;
            }
            if (m.getYear().equals(lastYear) && m.getMonth().equals(lastMonthValue)) {
                lastTotal = lastTotal.add(m.getTotalSettlement());
            }
        }
        BigDecimal growthRate = BigDecimal.ZERO;
        if(lastTotal.compareTo(BigDecimal.ZERO) == 0) {
            growthRate = BigDecimal.valueOf(currentTotal.compareTo(BigDecimal.ZERO));
        }else {
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
                currentTotalRefund,
                currentTotalSettlement,
                growthRate.doubleValue(),
                currentCount
        );
    }

    public void getMonthlySettlement(Long userId) {
        List<WeeklySettlement> settlements = weeklySettlementRepository.findByWeeklySettlement(userId);
        if (settlements == null || settlements.isEmpty()) {
            System.out.println("조회된 정산 데이터가 없어요");
        }
        Short prevYear = null;
        Byte prevMonth = null;

        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalSettlement = BigDecimal.ZERO;

        for (WeeklySettlement w : settlements) {
            short year =  w.getYear();
            byte month = w.getMonth();

            if (prevYear != null && (prevYear != year || prevMonth != month)) {
                monthlySettlementRepository.save(
                        MonthlySettlement.builder()
                                .userId(userId)
                                .year(prevYear)
                                .month(prevMonth)
                                .totalSales(totalSales)
                                .totalFee(totalFee)
                                .totalRefund(totalRefund)
                                .totalSettlement(totalSettlement)
                                .build()
                );
                totalSales = BigDecimal.ZERO;
                totalFee = BigDecimal.ZERO;
                totalRefund = BigDecimal.ZERO;
                totalSettlement = BigDecimal.ZERO;
            }
            totalSales = totalSales.add(w.getTotalSales());
            totalFee = totalFee.add(w.getTotalFee());
            totalRefund = totalRefund.add(w.getTotalRefund());
            totalSettlement = totalSettlement.add(w.getTotalSettlement());
            prevYear = year;
            prevMonth = month;
            MonthlySettlement monthlySettlement = MonthlySettlement.builder()
                    .userId(userId)
                    .year(prevYear != null ? prevYear : year)
                    .month(prevMonth != null ? prevMonth : month)
                    .totalSales(totalSales)
                    .totalFee(totalFee)
                    .totalRefund(totalRefund)
                    .totalSettlement(totalSettlement)
                    .build();
            System.out.println(":::::" + monthlySettlement.getTotalSettlement());
            monthlySettlementRepository.save(monthlySettlement);
        }
    }
}