package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.settlement.dto.response.YearlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.MonthlySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.YearlySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.MonthlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.YearlySettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class YearlySettlementService {
    private final YearlySettlementRepository yearlySettlementRepository;
    private final MonthlySettlementRepository monthlySettlementRepository;
    private final SettlementRepository settlementRepository;

    // 연별 데이터 조회
    @Transactional(readOnly = true)
    public Page<YearlySettlementResponse> getYearlySummary(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // yyyy-01-01 <=  ~ < yyyy-01-01
        LocalDate fromDate   = LocalDate.of(startDate.getYear(), 1, 1);
        LocalDate toDate   = LocalDate.of((short)(endDate.getYear() + 1), 1, 1);
        LocalDateTime start  = fromDate.atStartOfDay();
        LocalDateTime end  = toDate.atStartOfDay();

        short fromYear = (short) startDate.getYear();
        short toYearEx = (short) (endDate.getYear() + 1);

        // 1. 페이지로 정산일시 기준 연별 정산목록 조회
        Page<YearlySettlement> yearlySettlements = yearlySettlementRepository.findByYearlySettlementByRange(userId, fromYear, toYearEx, pageable);

        // 2. 기간 전체의 총 주문 건수
        long totalCount = settlementRepository.countAllByOrderedAt(userId, start, end);

        // 3. 데이터가 존재하지 않으면 0 반환
        if (yearlySettlements.isEmpty()) {
            new YearlySettlementResponse(
                    (short) startDate.getYear(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L
            );
        }

        // 4. 응답 반환
        List<YearlySettlementResponse> yearlySettlement = new ArrayList<>(yearlySettlements.getNumberOfElements());
        for (YearlySettlement y : yearlySettlements.getContent()) {
            yearlySettlement.add(new YearlySettlementResponse(
                    y.getYear(),
                    y.getTotalSales(),
                    y.getTotalFee(),
                    y.getTotalVat(),
                    y.getTotalRefund(),
                    y.getTotalSettlement(),
                    totalCount
            ));
        }
        return new PageImpl<>(yearlySettlement, pageable, totalCount);
    }

    // 연별 집계
    public void getYearlySettlement(Long userId) {
        Short prevYear = null;
        List<MonthlySettlement> settlements = monthlySettlementRepository.findByMonthlySettlement(userId);
        if (settlements == null || settlements.isEmpty()) {
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND);
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
        if (prevYear != null) {
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