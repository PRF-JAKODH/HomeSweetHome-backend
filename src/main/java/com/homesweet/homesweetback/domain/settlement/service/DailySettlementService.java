package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.domain.settlement.dto.response.DailySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    public DailySettlementResponse getDailySummary(Long userId, LocalDate date){

        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.atTime(23, 59, 59);

        List<Settlement> settlements = settlementRepository.findByUserIdAndOrderedAtBetween(userId, startDate, endDate);
        if (settlements.isEmpty()) {
            return new DailySettlementResponse(
                    date, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, 0.0, 0
            );
        }

        int completedCount = 0;
        int totalCount = settlements.size();
        BigDecimal totalSales   = BigDecimal.ZERO;
        BigDecimal totalFee     = BigDecimal.ZERO;
        BigDecimal totalVat     = BigDecimal.ZERO;
        BigDecimal totalRefund  = BigDecimal.ZERO;
        BigDecimal totalSettlement = BigDecimal.ZERO;

        for (Settlement s : settlements) {
            totalSales       = totalSales.add(s.getSalesAmount());
            totalFee         = totalFee.add(s.getFee());
            totalVat         = totalVat.add(s.getVat());
            totalRefund      = totalRefund.add(s.getRefundAmount());

            if (Objects.equals(s.getSettlementStatus(), "COMPLETED")) {
                completedCount++;
            }
        }
        double completedRate = (double) completedCount / totalCount * 100.0;
        return new DailySettlementResponse(
                date,
                totalSales,
                totalFee,
                totalVat,
                totalRefund,
                totalSettlement,
                Math.round(completedRate * 10) / 10.0,
                totalCount
        );
    }
    // 일별 조회
    public List<DailySettlement> getSettlement(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Settlement> settlements = settlementRepository.findBySettlement(startDate, endDate);
        if (settlements == null || settlements.isEmpty()) {
            System.out.println("조회된 정산 데이터가 없어요");
        }
        LocalDate prevOrderedAt = null;
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalSettlement = BigDecimal.ZERO;

        for (Settlement s : settlements) {
            LocalDate orderedAt = s.getOrder().getOrderedAt().toLocalDate();
            if (prevOrderedAt != null && !orderedAt.equals(prevOrderedAt)) {
                DailySettlement saved = dailySettlementRepository.save(
                        DailySettlement.builder()
                                .userId(s.getUserId())
                                .settlementDate(prevOrderedAt.atStartOfDay())  // 현재 누적하고 있던 날짜 기준 저장
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
            totalSales = s.getSalesAmount().add(totalSales);
            totalFee = s.getFee().add(totalFee);
            totalVat = s.getVat().add(totalVat);
            totalRefund = s.getRefundAmount().add(totalRefund);
            totalSettlement = s.getSettlementAmount().add(totalSettlement);

            prevOrderedAt = orderedAt;
        }
        DailySettlement dailySettlement = DailySettlement.builder()
                .userId(userId)
                .totalSales(totalSales)
                .totalFee(totalFee)
                .totalVat(totalVat)
                .totalRefund(totalRefund)
                .totalSettlement(totalSettlement)
                .settlementDate(prevOrderedAt.atStartOfDay())
                .build();
        dailySettlementRepository.save(dailySettlement);
        return List.of();
    }
    // 정산 상태별 조회
    public List<Settlement> getDailySettlementStatus(Long userId, LocalDate date, String settlementStatus) {
        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.atTime(23, 59, 59);
        return settlementRepository.findByUserIdAndOrderOrderedAtBetweenAndSettlementStatus(userId, startDate, endDate, settlementStatus);
    }
}