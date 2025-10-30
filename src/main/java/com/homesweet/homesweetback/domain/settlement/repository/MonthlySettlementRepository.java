package com.homesweet.homesweetback.domain.settlement.repository;

import com.homesweet.homesweetback.domain.settlement.entity.MonthlySettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MonthlySettlementRepository extends JpaRepository<MonthlySettlement, Long> {
    // 월별 정산 집계 조회
    @Query("SELECT m FROM MonthlySettlement m WHERE m.userId = :userId")
    List<MonthlySettlement> findByMonthlySettlement(@Param("userId") Long userId);
}
