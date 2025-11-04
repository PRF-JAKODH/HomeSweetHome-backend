package com.homesweet.homesweetback.domain.settlement.repository;

import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    int countByUserIdAndSettlementDateRange(Long userId, LocalDateTime start, LocalDateTime endEx);


}
