package com.homesweet.homesweetback.domain.settlement.repository;

import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
            (user_id, year, month, weekStartDate, weekEndDate, dailySales, weeklySales,  totalSales, totalFee, totalVat, totalRefund, totalSettlement) 
    VALUES
            (:userId, :year, :month, :weekStartDate, :weekEndDate, :dailySales, :weeklySales, :totalSales, :totalFee, :totalVat, :totalRefund, :totalSettlement) 
    ON DUPLICATE KEY UPDATE
            total_sales = total_sales + VALUES(total_sales),
            total_fee = total_fee + VALUES(total_fee),
            total_vat = total_vat + VALUES(total_vat),
            total_refund = total_refund + VALUES(total_refund),
            total_settlement = total_settlement + VALUES(total_settlement)
    
    """, nativeQuery = true)
    int upsertWeekly(
            @Param("userId") Long userId,
            @Param("year") Short year,
            @Param("month") Byte month,
            @Param("weekStartDate") LocalDate weekStartDate,
            @Param("weekEndDate") LocalDate weekEndDate,
            @Param("dailySales")  BigDecimal dailySales,
            @Param("weeklySales") BigDecimal weeklySales,
            @Param("totalSales") BigDecimal totalSales,
            @Param("totalFee") BigDecimal totalFee,
            @Param("totalVat") BigDecimal totalVat,
            @Param("totalRefund") BigDecimal totalRefund,
            @Param("totalSettlement") BigDecimal totalSettlement
    );
}
