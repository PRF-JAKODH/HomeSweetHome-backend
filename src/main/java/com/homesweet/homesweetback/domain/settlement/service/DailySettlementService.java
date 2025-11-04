package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.domain.settlement.dto.response.DailySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.awt.print.Pageable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DailySettlementService {
    private final DailySettlementRepository dailySettlementRepository;
    private final SettlementRepository settlementRepository;

    //일별 요약 조회 + 증감률
    public List<DailySettlementResponse> getDailySummary(Long userId, LocalDate date) {

        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.atTime(23, 59, 59);

        List<Settlement> settlements = settlementRepository.findByUserIdAndOrderedAtBetween(userId, startDate, endDate);
        if (settlements.isEmpty()) {
            DailySettlementResponse empty = new DailySettlementResponse(
                    date, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, date, "CANCELED", 0.0, 0, true
            );
            return List.of(empty);
        }

        int completedCount = 0;
        int totalCount = settlements.size();
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalSettlement = BigDecimal.ZERO;

        for (Settlement s : settlements) {
            totalSales = totalSales.add(s.getSalesAmount());
            totalFee = totalFee.add(s.getFee());
            totalVat = totalVat.add(s.getVat());
            totalRefund = totalRefund.add(s.getRefundAmount());
            totalSettlement = totalSettlement.add(s.getSettlementAmount());

            if (Objects.equals(s.getSettlementStatus(), "COMPLETED")) {
                completedCount++;
            }
        }
        double completedRate = (double) completedCount / totalCount * 100.0;
        // 기본은 PENDING
        String settlementStatus = "PENDING";
        if (completedCount == totalCount) {
            settlementStatus = "COMPLETED";
        }

        DailySettlementResponse dto = new DailySettlementResponse(
                date,
                totalSales,
                totalFee,
                totalVat,
                totalRefund,
                totalSettlement,
                date,
                settlementStatus,
                Math.round(completedRate * 10) / 10.0,
                totalCount,
                false
        );
        return List.of(dto);
    }

    // 일별 조회(정산일 기준)
    public void getSettlement(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        LocalDate prevDate = null; // 정산 일자 기준
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalSettlement = BigDecimal.ZERO;

        List<Settlement> settlements = settlementRepository
                .findBySettlementDateRange(userId, startDate, endDate);
        System.out.println("[Daily--] userId=" + userId
                + " range=[" + startDate + " ~ " + endDate + "]");

        if (settlements == null || settlements.isEmpty()) {
            System.out.println("조회된 정산 데이터가 없어요");
            return;
        }
        for (Settlement s : settlements) {
            LocalDate stDate = s.getSettlementDate().toLocalDate();
            System.out.println("---" + stDate);
            // 날짜가 바뀌면 upsert
            if (prevDate != null && !stDate.equals(prevDate)) {
                dailySettlementRepository.upsertDaily(
                        userId, prevDate.atStartOfDay(),     // 자정 고정
                        totalSales, totalFee, totalVat, totalRefund, totalSettlement
                );
                totalSales = BigDecimal.ZERO;
                totalFee = BigDecimal.ZERO;
                totalVat = BigDecimal.ZERO;
                totalRefund = BigDecimal.ZERO;
                totalSettlement = BigDecimal.ZERO;
            }
            totalSales = s.getSalesAmount().add(totalSales);
            totalFee = s.getFee().add(totalFee);
            totalVat = s.getVat().add(totalVat);
            totalRefund = s.getRefundAmount().add(totalRefund);
            totalSettlement = s.getSettlementAmount().add(totalSettlement);

            prevDate = stDate;
        }
        System.out.println("upsertupsertupsert");
        if (prevDate != null) {
            dailySettlementRepository.upsertDaily(
                    userId, prevDate.atStartOfDay(),     // 자정 고정
                    totalSales, totalFee, totalVat, totalRefund, totalSettlement
            );
        }
    }

    // 정산 상태별 조회
    public List<Settlement> getDailySettlementStatus(Long userId, LocalDate date, String settlementStatus) {
        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.atTime(23, 59, 59);
        return settlementRepository.findByUserIdAndOrderOrderedAtBetweenAndSettlementStatus(userId, startDate, endDate, settlementStatus);
    }

}