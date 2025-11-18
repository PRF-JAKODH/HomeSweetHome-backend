package com.homesweet.homesweetback.domain.settlement.repository;

import com.homesweet.homesweetback.domain.settlement.entity.MonthlySettlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MonthlySettlementRepository extends JpaRepository<MonthlySettlement, Long> {
    // 월별 정산 집계 조회
    @Query("SELECT m FROM MonthlySettlement m WHERE m.userId = :userId")
    List<MonthlySettlement> findByMonthlySettlement(@Param("userId") Long userId);
//    @Query("SELECT m FROM MonthlySettlement m WHERE m.userId = :userId AND m.year =:year AND m.month =:month")
//    List<MonthlySettlement> findByMonthlySettlement(@Param("userId") Long userId, @Param("year") Short year, @Param("month") Byte month);

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
