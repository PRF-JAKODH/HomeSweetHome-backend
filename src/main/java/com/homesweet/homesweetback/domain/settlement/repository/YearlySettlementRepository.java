package com.homesweet.homesweetback.domain.settlement.repository;

import com.homesweet.homesweetback.domain.settlement.entity.YearlySettlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
