package com.homesweet.homesweetback.domain.settlement.repository;

import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface DailySettlementRepository extends JpaRepository<DailySettlement, Long> {
    // 일별 집계 조회
    @Query("SELECT d FROM DailySettlement d WHERE d.userId = :userId")
    List<DailySettlement> findByDailySettlement(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(d) FROM DailySettlement d
            WHERE d.userId = :userId
              AND d.settlementDate >= :start
              AND d.settlementDate <  :endEx
            """)
    int countByUserIdInRange(Long userId, LocalDateTime start, LocalDateTime endEx);


    // upsert(update로 가야함)
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO daily_settlements
          (user_id, settlement_date, total_sales, total_fee, total_vat, total_refund, total_settlement)
        VALUES
          (:userId, :settlementDate, :totalSales, :totalFee, :totalVat, :totalRefund, :totalSettlement)
        AS new
        ON DUPLICATE KEY UPDATE
            total_sales = new.total_sales,
            total_fee = new.total_fee,
            total_vat = new.total_vat,
            total_refund = new.total_refund,
            total_settlement = new.total_settlement
        """, nativeQuery = true)
    int upsertDaily(
            @Param("userId") Long userId,
            @Param("settlementDate") LocalDateTime settlementDate,   // 자정(00:00:00)로 넣기
            @Param("totalSales") BigDecimal totalSales,
            @Param("totalFee") BigDecimal totalFee,
            @Param("totalVat") BigDecimal totalVat,
            @Param("totalRefund") BigDecimal totalRefund,
            @Param("totalSettlement") BigDecimal totalSettlement
    );

    // 일별 집계 조회
    @Query(value = """
    SELECT d FROM DailySettlement d WHERE d.userId =:userId
        AND d.settlementDate >= :startDate AND d.settlementDate < :endDate
        ORDER BY d.settlementDate DESC
    """)
    Page<DailySettlement> findByDailySettlementByRange(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);
}
