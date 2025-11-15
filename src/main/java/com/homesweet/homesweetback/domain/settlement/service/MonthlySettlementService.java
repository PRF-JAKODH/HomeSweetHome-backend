package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.settlement.dto.response.MonthlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.dto.response.YearlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.MonthlySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.MonthlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.WeeklySettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthlySettlementService {
    private final MonthlySettlementRepository monthlySettlementRepository;
    private final WeeklySettlementRepository weeklySettlementRepository;
    private final SettlementRepository settlementRepository;

    // 월별 데이터 조회(페이지 처리)
    @Transactional(readOnly = true)
    public Page<MonthlySettlementResponse> getMonthlySummary(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        YearMonth fromYM = YearMonth.from(startDate);
        YearMonth toYM   = YearMonth.from(endDate);

        LocalDate fromInclusive = fromYM.atDay(1);                // 해당 월 1일 00:00:00
        LocalDate toExclusive   = toYM.plusMonths(1).atDay(1);    // 다음 달 1일 00:00:00

        LocalDateTime from = fromInclusive.atStartOfDay(); // 00:00:00
        LocalDateTime toEx = toExclusive.atStartOfDay();

        short fromYear = (short) fromYM.getYear();
        byte  fromMonth = (byte) fromYM.getMonthValue();
        short toYear   = (short) toYM.getYear();
        byte  toMonth  = (byte) toYM.getMonthValue();

        long totalCount = settlementRepository.countAllByOrderedAt(userId, from, toEx);

        Page<MonthlySettlement> monthlySettlements = monthlySettlementRepository.findByMonthlySettlementByRange(userId, fromYear, fromMonth, toYear, toMonth, pageable);

        if (monthlySettlements.isEmpty()) {
            MonthlySettlementResponse empty = new MonthlySettlementResponse(
                    (short) fromYM.getYear(),
                    (byte) fromYM.getMonthValue(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    0.0, 0L
            );
            return new  PageImpl<>(List.of(empty), pageable, totalCount);
        }
        // 4. 응답 반환
        List<MonthlySettlementResponse> monthlySettlement = new ArrayList<>(monthlySettlements.getNumberOfElements());
        BigDecimal prevTotal = null;
        for (MonthlySettlement m : monthlySettlements.getContent()) {
            BigDecimal currTotal = m.getTotalSales();

            double growthRate;
            if (prevTotal == null || prevTotal.compareTo(BigDecimal.ZERO) == 0) {
                growthRate = 0.0;
            } else {
                growthRate = currTotal.subtract(prevTotal)
                        .divide(prevTotal, 1, RoundingMode.HALF_UP) // 소수 1자리
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
            }

            monthlySettlement.add(new MonthlySettlementResponse(
                    m.getYear(),
                    m.getMonth(),
                    m.getTotalSales(),
                    m.getTotalFee(),
                    m.getTotalVat(),
                    m.getTotalRefund(),
                    m.getTotalSettlement(),
                    growthRate,
                    totalCount
            ));
            prevTotal = currTotal;
        }
        return new PageImpl<>(monthlySettlement, pageable, totalCount);
    }

    // 월별 집계
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
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND);
        }
        for (WeeklySettlement w : settlements) {
            Short year = w.getYear();
            Byte month = w.getMonth();
            // 첫번째 데이터 초기화
            if (prevYear == null) {
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
                totalSales = BigDecimal.ZERO;
                totalFee = BigDecimal.ZERO;
                totalVat = BigDecimal.ZERO;
                totalRefund = BigDecimal.ZERO;
                totalSettlement = BigDecimal.ZERO;
            }
            // 현재 월의 누적
            // 메소드로 변경하기!!
            totalSales = totalSales.add(w.getTotalSales());
            totalFee = totalFee.add(w.getTotalFee());
            totalVat = totalVat.add(w.getTotalVat());
            totalRefund = totalRefund.add(w.getTotalRefund());
            totalSettlement = totalSettlement.add(w.getTotalSettlement());

        }
        // 마지막 월 upsert
        if (prevMonth != null) {
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