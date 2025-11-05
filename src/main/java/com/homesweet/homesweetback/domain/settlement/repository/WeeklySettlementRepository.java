package com.homesweet.homesweetback.domain.settlement.repository;

import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface WeeklySettlementRepository extends JpaRepository<WeeklySettlement, Long> {
    // 주별 조회
    @Query("SELECT w FROM WeeklySettlement w WHERE w.userId = :userId")
    List<WeeklySettlement> findByWeeklySettlement(@Param("userId") Long userId);


    @Query("""
            SELECT w FROM WeeklySettlement w
            WHERE w.userId = :userId AND w.year = :year AND w.month = :month
            ORDER BY w.weekStartDate ASC""")
    List<WeeklySettlement> findByUserIdAndYearAndMonthOrderByWeek(Long userId, Short year, Byte month);

    // upsert
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO weekly_settlements
              (user_id, `year`, `month`, week_start_date, week_end_date,
               total_sales, total_fee, total_vat, total_refund, total_settlement)
            SELECT
              new.user_id, new.`year`, new.`month`, new.week_start_date, new.week_end_date,
              new.total_sales, new.total_fee, new.total_vat, new.total_refund, new.total_settlement
            FROM (
              SELECT
                :userId                              AS user_id,
                YEAR(:weekStartDate)                 AS `year`,
                MONTH(:weekStartDate)                AS `month`,
                :weekStartDate                       AS week_start_date,
                DATE_ADD(:weekStartDate, INTERVAL 6 DAY) AS week_end_date,
                COALESCE(SUM(d.total_sales),      0) AS total_sales,
                COALESCE(SUM(d.total_fee),        0) AS total_fee,
                COALESCE(SUM(d.total_vat),        0) AS total_vat,
                COALESCE(SUM(d.total_refund),     0) AS total_refund,
                COALESCE(SUM(d.total_settlement), 0) AS total_settlement
              FROM daily_settlements d
              WHERE d.user_id = :userId
                AND d.settlement_date >= :weekStartDate
                AND d.settlement_date <  DATE_ADD(:weekStartDate, INTERVAL 7 DAY)
            ) AS new
            ON DUPLICATE KEY UPDATE
              total_sales      = new.total_sales,
              total_fee        = new.total_fee,
              total_vat        = new.total_vat,
              total_refund     = new.total_refund,
              total_settlement = new.total_settlement,
              `month`          = new.`month`,
              week_end_date    = new.week_end_date;
            """, nativeQuery = true)
    int upsertWeekly(
            @Param("userId") Long userId,
            @Param("year")  Short year,
            @Param("month") Byte month,
            @Param("weekStartDate") LocalDate weekStartDate,
            @Param("weekEndDate") LocalDate weekEndDate,
            @Param("totalSales") BigDecimal totalSales,
            @Param("totalFee") BigDecimal totalFee,
            @Param("totalVat") BigDecimal totalVat,
            @Param("totalRefund") BigDecimal totalRefund,
            @Param("totalSettlement") BigDecimal totalSettlement
    );
}
