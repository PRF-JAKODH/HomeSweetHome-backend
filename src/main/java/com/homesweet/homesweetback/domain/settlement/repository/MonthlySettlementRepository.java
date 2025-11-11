package com.homesweet.homesweetback.domain.settlement.repository;

import com.homesweet.homesweetback.domain.settlement.dto.response.MonthlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.MonthlySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MonthlySettlementRepository extends JpaRepository<MonthlySettlement, Long> {
    // 월별 정산 집계 조회
    @Query("SELECT m FROM MonthlySettlement m WHERE m.userId = :userId")
    List<MonthlySettlement> findByMonthlySettlement(@Param("userId") Long userId);
//    @Query("SELECT m FROM MonthlySettlement m WHERE m.userId = :userId AND m.year =:year AND m.month =:month")
//    List<MonthlySettlement> findByMonthlySettlement(@Param("userId") Long userId, @Param("year") Short year, @Param("month") Byte month);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO monthly_settlements
          (user_id, `year`, `month`, total_sales, total_fee, total_vat, total_refund, total_settlement)
        SELECT
          new.user_id, new.`year`, new.`month`,
          new.total_sales, new.total_fee, new.total_vat, new.total_refund, new.total_settlement
        FROM (
          SELECT
            :userId                               AS user_id,
            :year                                 AS `year`,
            :month                                AS `month`,
            COALESCE(SUM(w.total_sales),      0)  AS total_sales,
            COALESCE(SUM(w.total_fee),        0)  AS total_fee,
            COALESCE(SUM(w.total_vat),        0)  AS total_vat,
            COALESCE(SUM(w.total_refund),     0)  AS total_refund,
            COALESCE(SUM(w.total_settlement), 0)  AS total_settlement
          FROM weekly_settlements w
          WHERE w.user_id = :userId
            AND w.`year`  = :year
            AND w.`month` = :month
        ) AS new
        ON DUPLICATE KEY UPDATE
          total_sales      = new.total_sales,
          total_fee        = new.total_fee,
          total_vat        = new.total_vat,
          total_refund     = new.total_refund,
          total_settlement = new.total_settlement,
          `month`          = new.`month`
    """, nativeQuery = true)
    int upsertMonthly(
            @Param("userId") Long userId,
            @Param("year")  Short year,
            @Param("month") Byte month,
            @Param("totalSales") BigDecimal totalSales,
            @Param("totalFee") BigDecimal totalFee,
            @Param("totalVat") BigDecimal totalVat,
            @Param("totalRefund") BigDecimal totalRefund,
            @Param("totalSettlement") BigDecimal totalSettlement
    );
    // 월별 집계 조회
    @Query("""
    SELECT m FROM MonthlySettlement m
    WHERE m.userId = :userId
     AND (m.year >= :fromYear OR (m.year = :fromYear AND m.month >= :fromMonth))
     AND (m.year < :toYear OR (m.year = :toYear AND m.month >= :toMonth))
    ORDER BY m.year DESC, m.month DESC
    """)
    Page<MonthlySettlement> findByMonthlySettlementByRange(@Param("userId") Long userId, @Param("fromYear") Short fromYear, @Param("fromMonth") Byte fromMonth, @Param("toYear") Short toYear, @Param("toMonth") Byte toMonth, Pageable pageable);
}
