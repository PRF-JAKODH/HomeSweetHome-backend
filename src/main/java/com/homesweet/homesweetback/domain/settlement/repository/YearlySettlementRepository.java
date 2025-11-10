package com.homesweet.homesweetback.domain.settlement.repository;

import com.homesweet.homesweetback.domain.settlement.dto.response.YearlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.YearlySettlement;
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

public interface YearlySettlementRepository extends JpaRepository<YearlySettlement, Long> {
    // 특정 사용자의 기간별 집계 내역 조회
    List<YearlySettlement> findByUserIdAndYearBetweenOrderByYearDesc(
            Long userId, Short startDate, Short endDate
    );
    // 연별 집계 조회
    List<YearlySettlement> findByUserIdOrderByYearDesc(Long userId);

    @Query("SELECT y FROM YearlySettlement y WHERE y.userId = :userId")
    List<YearlySettlement> findByYearlySettlement(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO yearly_settlements
              (user_id, `year`, total_sales, total_fee, total_vat, total_refund, total_settlement)
            SELECT
              new.user_id, new.`year`, new.total_sales, new.total_fee, new.total_vat, new.total_refund, new.total_settlement
            FROM (
              SELECT
                :userId                              AS user_id,
                :year                                AS `year`,
                COALESCE(SUM(m.total_sales),      0) AS total_sales,
                COALESCE(SUM(m.total_fee),        0) AS total_fee,
                COALESCE(SUM(m.total_vat),        0) AS total_vat,
                COALESCE(SUM(m.total_refund),     0) AS total_refund,
                COALESCE(SUM(m.total_settlement), 0) AS total_settlement
              FROM monthly_settlements m
              WHERE m.user_id = :userId
                AND m.`year` = :year
            ) AS new
            ON DUPLICATE KEY UPDATE
              total_sales      = new.total_sales,
              total_fee        = new.total_fee,
              total_vat        = new.total_vat,
              total_refund     = new.total_refund,
              total_settlement = new.total_settlement
    """, nativeQuery = true)
    int upsertYearly(
            @Param("userId") Long userId,
            @Param("year") Short year,
            @Param("totalSales") BigDecimal totalSales,
            @Param("totalFee") BigDecimal totalFee,
            @Param("totalVat") BigDecimal totalVat,
            @Param("totalRefund") BigDecimal totalRefund,
            @Param("totalSettlement") BigDecimal totalSettlement
    );

    // 일별 집계 조회
    @Query("""
    SELECT y
    FROM YearlySettlement y
    WHERE y.userId = :userId
      AND y.year >= :fromYear
      AND y.year < :toYearEx
    ORDER BY y.year DESC
    """)
    Page<YearlySettlement> findByYearlySettlementByRange(
            @Param("userId") Long userId,
            @Param("fromYear") Short fromYear,
            @Param("toYearEx") Short toYearEx,
            Pageable pageable
    );
}
