package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.settlement.dto.response.DailySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.mapper.DailySettlementMapper;
import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.DailyResponseFactory;
import com.homesweet.homesweetback.domain.settlement.util.DailySettlementCalculator;
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
    private final DailySettlementCalculator dailySettlementCalculator;
    private final DailyResponseFactory dailyResponseFactory;
    private final DailySettlementMapper dailySettlementMapper;

    // 일별 데이터 조회 (페이지 처리)
    @Transactional(readOnly = true)
    public Page<DailySettlementResponse> getDailySummary(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // 주문일시 날짜[start, end] 끝일 포함
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        // 1. 페이지로 정산일시 기준 정산 목록 조회(실제 리스트)
        Page<DailySettlement> dailySettlements = findDailySettlements(userId, pageable, start, end);

        // 2. 기간 전체의 총 주문 건수/총 정산 완료 건수/정산 완료율 계산
        DailySettlementCalculator.SettlementStats stats = dailySettlementCalculator.calculateStats(userId, startDate, endDate);

        // 3. 데이터가 존재하지 않으면 0 반환
        if (dailySettlements.isEmpty()) {
            return dailyResponseFactory.createEmptyDaily(startDate, pageable);
        }

        // 4. 페이지의 실제 리스트를 response에 매핑
        List<DailySettlementResponse> dailySettlement = dailySettlementMapper.toDailySettlementResponseList(
                dailySettlements.getContent(),
                stats.totalCount(),
                stats.completedCount(),
                stats.completedRate()
        );

        return new PageImpl<>(dailySettlement, pageable, stats.totalCount());   // 전체 페이지수
    }

    public Page<DailySettlement> findDailySettlements(Long userId, Pageable pageable, LocalDateTime start, LocalDateTime end) {
        Page<DailySettlement> dailySettlements = dailySettlementRepository.findByDailySettlementByRange(userId, start, end, pageable);
        return dailySettlements;
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
}