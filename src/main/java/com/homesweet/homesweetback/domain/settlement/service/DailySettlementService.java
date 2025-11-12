package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.settlement.dto.response.DailySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DailySettlementService {
    private static final Logger log = LogManager.getLogger(DailySettlementService.class);
    private final DailySettlementRepository dailySettlementRepository;
    private final SettlementRepository settlementRepository;

    // 일별 데이터 조회 (페이지 처리)
    @Transactional(readOnly = true)
    public Page<DailySettlementResponse> getDailySummary(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // 주문일시 날짜[start, end] 끝일 포함
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        // 1. 페이지로 정산일시 기준 정산 목록 조회(실제 리스트)
        Page<DailySettlement> dailySettlements = dailySettlementRepository.findByDailySettlementByRange(userId, start, end, pageable);
        if (!dailySettlements.isEmpty()) {
            DailySettlement first = dailySettlements.getContent().get(0);
        } else {
            log.warn("EMPTY PAGE -> will return zero row");
        }
        // 2. 기간 전체의 총 주문 건수/총 정산 완료 건수/정산 완료율 계산
        long totalCount = settlementRepository.countAllByOrderedAt(userId, start, end);  // 총 주문건수
        long completedCount = settlementRepository.countCompletedSettlements(userId, start, end);
        double completedRate = totalCount == 0 ? 0.0 : Math.round(((double) completedCount * 100.0 / totalCount) * 10) / 10.0;  // 정산 완료율

        // 3. 데이터가 존재하지 않으면 0 반환
        if (dailySettlements.isEmpty()) {
           DailySettlementResponse empty = new DailySettlementResponse(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, startDate, "CANCELED", 0.0, 0L
            );
            return new PageImpl<>(List.of(empty), pageable, 1);
        }
        // 일별 조회의 요소
        List<DailySettlementResponse> dailySettlement = new ArrayList<>(dailySettlements.getNumberOfElements());

        // 4. 페이지의 실제 리스트를 response에 매핑
        for (DailySettlement d : dailySettlements.getContent()) { // 페이지의 실제 리스트
            LocalDate settlementDate = d.getSettlementDate().toLocalDate(); // 정산일시

            // 기본은 PENDING
            String settlementStatus = (completedCount == totalCount) ? "COMPLETED" : "PENDING";
            dailySettlement.add(new DailySettlementResponse(
                    d.getTotalSales(),
                    d.getTotalFee(),
                    d.getTotalVat(),
                    d.getTotalRefund(),
                    d.getTotalSettlement(),
                    settlementDate,
                    settlementStatus,
                    completedRate,
                    totalCount
            ));
        }
        return new PageImpl<>(dailySettlement, pageable, totalCount);   // 전체 페이지수
    }

    // 일별 집계
    @Transactional
    public void getSettlement(Long userId, LocalDateTime dailyStartDate, LocalDateTime dailyEndDate) {
        LocalDate prevDate = null; // 정산 일자 기준
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalSettlement = BigDecimal.ZERO;
//        String settlementStatus = "PENDING";

        // 1. 정산일 기준 정산 목록 조회
        List<Settlement> settlements = settlementRepository
                .findBySettlementDateRange(userId, dailyStartDate, dailyEndDate);

        // 2. 정산 내역이 없으면 에러 반환
        if (settlements == null || settlements.isEmpty()) {
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND);
        }

        // 3. 날짜별로 집계
        for (Settlement s : settlements) {
            // 정산일
            LocalDate stDate = s.getSettlementDate().toLocalDate();
            // 날짜가 바뀌면 upsert
            if (prevDate != null && !stDate.equals(prevDate)) {
                dailySettlementRepository.upsertDaily(
                        userId, prevDate.atStartOfDay(),
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
        if (prevDate != null) {
            dailySettlementRepository.upsertDaily(
                    userId, prevDate.atStartOfDay(),     // 자정 고정
                    totalSales, totalFee, totalVat, totalRefund, totalSettlement
            );
            // 정산 상태 변경 -> 'COMPLETED'
            settlementRepository.markCompletedInRange(userId, dailyStartDate, dailyEndDate);
        }
    }

    // 정산 상태 검증
//    private static final Set<String> ALLOWED_STATUS = Set.of("PENDING", "COMPLETED", "CANCELED");
//    if(settlementStatus != null && !ALLOWED_STATUS.contains(settlementStatus)){
//        throw new BusinessException(ErrorCode.UNKNOWN_SETTLEMENT_STATUS);
//    }


}