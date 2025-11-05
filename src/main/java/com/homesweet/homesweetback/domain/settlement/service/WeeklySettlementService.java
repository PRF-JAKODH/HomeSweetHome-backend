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

    public List<WeeklySettlementResponse> getWeeklySummary(Long userId, LocalDate date) {
        LocalDate startOfWeek = date.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = date.with(DayOfWeek.SUNDAY);

        LocalDateTime startDate = startOfWeek.atStartOfDay();
        LocalDateTime endDate = endOfWeek.atTime(23, 59, 59);

        List<Settlement> settlements = settlementRepository.findByUserIdAndOrderedAtBetween(userId, startDate, endDate);
        WeekFields wf = WeekFields.of(DayOfWeek.MONDAY, 4);
        int rawweek =  date.get(wf.weekOfMonth());
        Short week = (short) (rawweek < 1 ? 1 : rawweek);
        System.out.println("week:::: " + week);

        System.out.println("settlements: " + settlements);
        // 정산내역이 없다면 0.0
        if (settlements.isEmpty()) {
            WeeklySettlementResponse empty = new WeeklySettlementResponse(
                    (long) startOfWeek.getYear(),
                    (short) startOfWeek.getMonthValue(),
                    week,
                    startOfWeek,
                    endOfWeek,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    0.0, 0, true
            );
            return List.of(empty);
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
            totalVat = totalVat.add(s.getVat());
            totalRefund = totalRefund.add(s.getRefundAmount());
            totalSettlement = totalSettlement.add(s.getSettlementAmount());
        }
        double completedRate = (double) completedCount / totalCount * 100.0;
        WeeklySettlementResponse dto =  new WeeklySettlementResponse(
                (long) startOfWeek.getYear(),
                (short) startOfWeek.getMonthValue(),
                week,
                startOfWeek,
                endOfWeek,
                totalSales,
                totalFee,
                totalVat,
                totalRefund,
                totalSettlement,
                Math.round(completedRate * 10) / 10.0,
                totalCount,
                false
        );
        return List.of(dto);
    }


    // 주차별 정산내역
    public void getWeeklySettlement(Long userId, LocalDate weekStart, LocalDate weekEnd) {
        // 이전 연, 월, 주
        Short prevYear = null;
        Byte prevMonth = null;
        Integer prevWeek = null;

        LocalDate weekStartDate = null;
        LocalDate weekEndDate = null;

        // 일별 판매금액
        BigDecimal dailySales = null;
        BigDecimal weeklySales = null;

        // 주별 판매금액

        // 주 합계
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalSettlement = BigDecimal.ZERO;

        // 일자 합계
        BigDecimal lastDaySales  = BigDecimal.ZERO;

        List<DailySettlement> settlements = dailySettlementRepository.findByDailySettlement(userId);
        if (settlements == null || settlements.isEmpty()) {
            System.out.println("조회된 정산 데이터가 없어요");
        }

        for (DailySettlement s : settlements) {
            LocalDate stDate = s.getSettlementDate().toLocalDate();
            System.out.println("----" + stDate);
            WeekFields weekFields = WeekFields.of(DayOfWeek.MONDAY, 4); // 시작일이 월요일
            Short year = (short) stDate.getYear();
            Byte month = (byte) stDate.getMonthValue();
            int weekOfMonth = stDate.get(weekFields.weekOfMonth());

            LocalDate startOfWeek = stDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate endOfWeek = stDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

            // upsert
            if (prevYear != null && (!prevYear.equals(year) || !prevMonth.equals(month) || !prevWeek.equals(weekOfMonth))) {
                weeklySettlementRepository.upsertWeekly(userId, year, month, weekStartDate, weekEndDate, dailySales, weeklySales, totalSales, totalFee, totalVat, totalRefund, totalSettlement);
//                WeeklySettlement weeklySettlement = weeklySettlementRepository.save(
//                        WeeklySettlement.builder()
//                                .userId(s.getUserId())
//                                .year(prevYear)
//                                .month(prevMonth)
//                                .weekStartDate(weekStartDate.atStartOfDay().toLocalDate())
//                                .weekEndDate(weekEndDate.atTime(23, 59, 59).toLocalDate())
//                                .weeklySales(totalSales)
//                                .dailySales(lastDaySales)
//                                .totalSales(totalSales)
//                                .totalVat(totalVat)
//                                .totalFee(totalFee)
//                                .totalRefund(totalRefund)
//                                .totalSettlement(totalSettlement)
//                                .build()
//                );
                lastDaySales = s.getTotalSales();

                totalSales = BigDecimal.ZERO;
                totalFee = BigDecimal.ZERO;
                totalVat = BigDecimal.ZERO;
                totalRefund = BigDecimal.ZERO;
                totalSettlement = BigDecimal.ZERO;
            }
            totalSales = totalSales.add(s.getTotalSales());
            totalFee = totalFee.add(s.getTotalFee());
            totalVat = totalVat.add(s.getTotalVat());
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
                .dailySales(lastDaySales)
                .totalSales(totalSales)
                .totalFee(totalFee)
                .totalVat(totalVat)
                .totalRefund(totalRefund)
                .totalSettlement(totalSettlement)
                .build();
        weeklySettlementRepository.save(weeklySettlement);
    }
    private short calcWeekOfMonth(LocalDate date) {
        if (date == null) return 0;
        return (short) date.get(WeekFields.of(DayOfWeek.MONDAY, 4).weekOfMonth());
    }

//    public List<WeeklySettlementResponse> getWeeklyListOfMonth(Long userId, int year, int month) {
//        List<WeeklySettlement> weeks =
//                weeklySettlementRepository.findByUserIdAndYearAndMonthOrderByWeek(
//                        userId,
//                        (short) year,
//                        (byte) month
//                );
//
//        // 없으면 빈 리스트
//        if (weeks.isEmpty()) {
//            return List.of();
//        }
//
//        return weeks.stream()
//                .map(w -> new WeeklySettlementResponse(
//                        w.getYear().longValue(),
//                        w.getMonth().shortValue(),
//                        // 엔티티에 week 번호 필드 있으면 그거 쓰고, 없으면 startDate로 다시 계산
//                        calcWeekOfMonth(w.getWeekStartDate()),
//                        w.getWeekStartDate(),
//                        w.getWeekEndDate(),
//                        w.getTotalSales(),
//                        w.getTotalFee(),
//                        BigDecimal.ZERO,             // 주별 VAT 안 따로 저장했다면 0
//                        w.getTotalRefund(),
//                        w.getTotalSettlement(),
//                        0.0,                         // 완료율 따로 없으면 0
//                        0                            // 건수 따로 없으면 0
//                ))
//                .toList();
//    }

}
