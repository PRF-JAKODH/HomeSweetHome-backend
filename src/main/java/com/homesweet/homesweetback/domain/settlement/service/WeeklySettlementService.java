package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.settlement.dto.response.WeeklySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.WeeklySettlementRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WeeklySettlementService {
    private final WeeklySettlementRepository weeklySettlementRepository;
    private final SettlementRepository settlementRepository;
    private final DailySettlementRepository dailySettlementRepository;

    // 주별 데이터 조회
    @Transactional(readOnly = true)
    public Page<WeeklySettlementResponse> getWeeklySummary(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // 주 시작일: 월요일, 주 종료일: 일요일
        LocalDate firstWeekStart = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastWeekStartEx = endDate.plusDays(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 1. 페이지로 정산일시 기준 월별 정산목록 조회
        Page<WeeklySettlement> weeklySettlements = weeklySettlementRepository.findByWeeklySettlementByRange(userId, firstWeekStart, lastWeekStartEx, pageable);

        // 2. 기간 전체의 총 주문 건수/총 정산 완료 건수/정산 완료율
        LocalDateTime startDt = startDate.atStartOfDay();
        LocalDateTime endExDt = endDate.plusDays(1).atStartOfDay();
        long totalCount = settlementRepository.countAllByOrderedAt(userId, startDt, endExDt);
        long completedCount = settlementRepository.countCompletedSettlements(userId, startDt, endExDt);
        double completedRate = totalCount == 0 ? 0.0 : Math.round(((double) completedCount / totalCount * 100.0) + 10) / 10.0;
        // 3. 주차 구하기
        WeekFields wf = WeekFields.of(DayOfWeek.MONDAY, 4);
        byte week = (byte) firstWeekStart.get(wf.weekOfMonth());

        // 4. 데이터가 존재하지 않으면 0 반환
        if (weeklySettlements.isEmpty()) {
            new WeeklySettlementResponse(
                    (short) firstWeekStart.getYear(),
                    (byte) firstWeekStart.getMonthValue(),
                    week,
                    firstWeekStart,
                    firstWeekStart.plusDays(6),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    0.0, 0L
            );
        }

        // 5. 응답 반환
        List<WeeklySettlementResponse> weeklySettlement = new ArrayList<>(weeklySettlements.getNumberOfElements());
        for (WeeklySettlement w : weeklySettlements.getContent()) {
            weeklySettlement.add(new WeeklySettlementResponse(
                    w.getYear(),
                    w.getMonth(),
                    week,
                    w.getWeekStartDate(),
                    w.getWeekEndDate(),
                    w.getTotalSales(),
                    w.getTotalFee(),
                    w.getTotalVat(),
                    w.getTotalRefund(),
                    w.getTotalSettlement(),
                    completedRate,
                    totalCount
            ));
        }
        return new PageImpl<>(weeklySettlement, pageable, totalCount);
    }

    // 주차별 정산내역
    public void getWeeklySettlement(Long userId, LocalDate weekStart, LocalDate weekEnd) {
        // 이전 연, 월, 주
        Short prevYear = null;
        Byte prevMonth = null;
        Integer prevWeek = null; // 몇주
        LocalDate prevWeekStartDate = null;
        LocalDate prevWeekEndDate = null;

        // 주 합계
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalSettlement = BigDecimal.ZERO;

        // 1. 일별 집계내역 조회
        List<DailySettlement> settlements = dailySettlementRepository.findByDailySettlement(userId);
        if (settlements == null || settlements.isEmpty()) {
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND);
        }

        // 2.
        for (DailySettlement s : settlements) {
            LocalDate stDate = s.getSettlementDate().toLocalDate();

            // 현재 데이터의 주 시작일, 주 종료일
            WeekFields weekFields = WeekFields.of(DayOfWeek.MONDAY, 4); // 시작일이 월요일
            LocalDate startOfWeek = monday(stDate);
            LocalDate endOfWeek = sunday(stDate);

            Short year = (short) stDate.getYear();
            Byte month = (byte) stDate.getMonthValue();
            int weekOfMonth = stDate.get(weekFields.weekOfMonth());

            // 첫 주 초기화
            if (prevWeekStartDate == null) {
                prevWeekStartDate = startOfWeek;
                prevWeekEndDate = endOfWeek;
                prevYear = (short) startOfWeek.getYear();
                prevMonth = (byte) startOfWeek.getMonthValue();
            }
            // 주 시작일이 변경되면 upsert
            if (!startOfWeek.equals(prevWeekStartDate)) {
                weeklySettlementRepository.upsertWeekly(
                        userId,
                        prevYear,        // 직전 주의 연/월
                        prevMonth,
                        prevWeekStartDate,
                        prevWeekEndDate,
                        totalSales,
                        totalFee,
                        totalVat,
                        totalRefund,
                        totalSettlement
                );
                // 다음주
                prevWeekStartDate = startOfWeek;
                prevWeekEndDate = endOfWeek;
                prevYear = year;
                prevMonth = month;
                prevWeek = weekOfMonth;

                totalSales = BigDecimal.ZERO;
                totalFee = BigDecimal.ZERO;
                totalVat = BigDecimal.ZERO;
                totalRefund = BigDecimal.ZERO;
                totalSettlement = BigDecimal.ZERO;
            }
            // 현재 누적
            totalSales = totalSales.add(s.getTotalSales());
            totalFee = totalFee.add(s.getTotalFee());
            totalVat = totalVat.add(s.getTotalVat());
            totalRefund = totalRefund.add(s.getTotalRefund());
            totalSettlement = totalSettlement.add(s.getTotalSettlement());
        }
        // 마지막 주
        if (prevWeekStartDate != null) {
            weeklySettlementRepository.upsertWeekly(
                    userId,
                    prevYear,
                    prevMonth,
                    prevWeekStartDate,
                    prevWeekEndDate,
                    totalSales,
                    totalFee,
                    totalVat,
                    totalRefund,
                    totalSettlement
            );
        }
    }

    //    private short calcWeekOfMonth(LocalDate date) {
//        if (date == null) return 0;
//        return (short) date.get(WeekFields.of(DayOfWeek.MONDAY, 4).weekOfMonth());
//    }
    private static class WeekData {
        LocalDate weekStartDate;
        LocalDate weekEndDate;
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalSettlement = BigDecimal.ZERO;
        long totalCount = 0L;
    }
    // 주 시작일(월요일)
    private static LocalDate monday(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
    // 주 종료일(일요일)
    private static LocalDate sunday(LocalDate date) {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }
}