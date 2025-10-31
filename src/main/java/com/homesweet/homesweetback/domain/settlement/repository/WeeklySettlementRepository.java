package com.homesweet.homesweetback.domain.settlement.repository;

import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WeeklySettlementRepository extends JpaRepository<WeeklySettlement, Long> {
    // 주별 조회
    @Query("SELECT w FROM WeeklySettlement w WHERE w.userId = :userId")
    List<WeeklySettlement> findByWeeklySettlement(@Param("userId") Long userId);
}
