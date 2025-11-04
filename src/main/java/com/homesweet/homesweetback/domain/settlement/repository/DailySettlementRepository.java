package com.homesweet.homesweetback.domain.settlement.repository;

import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import jakarta.transaction.Transactional;
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
        ON DUPLICATE KEY UPDATE
          total_sales      = total_sales + VALUES(total_sales),
          total_fee        = total_fee + VALUES(total_fee),
          total_vat        = total_vat + VALUES(total_vat),
          total_refund     = total_refund + VALUES(total_refund),
          total_settlement = total_settlement + VALUES(total_settlement)
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
}
