package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.domain.settlement.dto.response.WeeklySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.WeeklySettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WeeklySettlementService {
    private final WeeklySettlementRepository weeklySettlementRepository;
    private final SettlementRepository settlementRepository;
    private final DailySettlementRepository dailySettlementRepository;

    public WeeklySettlementResponse getWeeklySummary(Long userId, LocalDate date) {
        LocalDate startOfWeek = date.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = date.with(DayOfWeek.SUNDAY);

        LocalDateTime startDate = startOfWeek.atStartOfDay();
        LocalDateTime endDate = endOfWeek.atTime(23, 59, 59);

        List<Settlement> settlements = settlementRepository.findByUserIdAndOrderedAtBetween(userId, startDate, endDate);
        System.out.println("settlements: " + settlements);
        // 정산내역이 없다면 0.0
        if (settlements.isEmpty()) {
            return new WeeklySettlementResponse(
                    (long) startOfWeek.getYear(),
                    (short) startOfWeek.getMonthValue(),
                    startOfWeek,
                    endOfWeek,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    0.0, 0
            );
        }
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalSettlement = BigDecimal.ZERO;
        int completedCount = 0;
        int totalCount = settlements.size();

        for (Settlement s : settlements) {
            if (Objects.equals(s.getSettlementStatus(), "COMPLETED")) {
                completedCount++;
            }
            totalSales = totalSales.add(s.getSalesAmount());
            totalFee = totalFee.add(s.getFee());
            totalRefund = totalRefund.add(s.getRefundAmount());
            totalSettlement = totalSettlement.add(s.getSettlementAmount());
        }
        double completedRate = (double) completedCount / totalCount * 100.0;
        return new WeeklySettlementResponse(
                (long) startOfWeek.getYear(),
                (short) startOfWeek.getMonthValue(),
                startOfWeek,
                endOfWeek,
                totalSales,
                totalFee,
                totalVat,
                totalRefund,
                totalSettlement,
                Math.round(completedRate * 10) / 10.0,
                totalCount
        );
    }


    // 주차별 정산내역
    public void getWeeklySettlement(Long userId) {
        List<DailySettlement> settlements = dailySettlementRepository.findByDailySettlement(userId);
        if (settlements == null || settlements.isEmpty()) {
            System.out.println("조회된 정산 데이터가 없어요");
        }
        // 이전 연, 월, 주
        Short prevYear = null;
        Byte prevMonth = null;
        Integer prevWeek = null;

        LocalDate weekStartDate = null;
        LocalDate weekEndDate = null;

        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalSettlement = BigDecimal.ZERO;

        for (DailySettlement s : settlements) {
            LocalDate orderedAt = s.getSettlementDate().toLocalDate();
            WeekFields weekFields = WeekFields.of(DayOfWeek.MONDAY, 4); // 시작일이 월요일

            Short year = (short) orderedAt.getYear();
            Byte month = (byte) orderedAt.getMonthValue();
            int weekOfMonth = orderedAt.get(weekFields.weekOfMonth());

            LocalDate startOfWeek = orderedAt.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate endOfWeek = orderedAt.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            if (prevYear != null && (!prevYear.equals(year) || !prevMonth.equals(month) || !prevWeek.equals(weekOfMonth))) {
                WeeklySettlement weeklySettlement = weeklySettlementRepository.save(
                        WeeklySettlement.builder()
                                .userId(s.getUserId())
                                .year(prevYear)
                                .month(prevMonth)
                                .weekStartDate(weekStartDate.atStartOfDay().toLocalDate())
                                .weekEndDate(weekEndDate.atTime(23, 59, 59).toLocalDate())
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
            totalSales = totalSales.add(s.getTotalSales());
            totalFee = totalFee.add(s.getTotalFee());
            totalRefund = totalRefund.add(s.getTotalRefund());
            totalSettlement = totalSettlement.add(s.getTotalSettlement());

            prevYear = year;
            prevMonth = month;
            prevWeek = weekOfMonth;
            weekStartDate = startOfWeek;
            weekEndDate = endOfWeek;
        }
        WeeklySettlement weeklySettlement = WeeklySettlement.builder()
                .userId(userId)
                .year(prevYear)
                .month(prevMonth)
                .weekStartDate(weekStartDate.atStartOfDay().toLocalDate())
                .weekEndDate(weekEndDate.atTime(23, 59, 59).toLocalDate())
                .weeklySales(totalSales)
                .totalSales(totalSales)
                .totalFee(totalFee)
                .totalRefund(totalRefund)
                .totalSettlement(totalSettlement)
                .build();
        weeklySettlementRepository.save(weeklySettlement);
    }
}
