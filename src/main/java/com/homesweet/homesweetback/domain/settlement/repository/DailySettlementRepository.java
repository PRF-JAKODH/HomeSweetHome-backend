package com.homesweet.homesweetback.domain.settlement.repository;

import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailySettlementRepository extends JpaRepository<DailySettlement, Long> {
    // 특정 판매자의 정산 내역을 기간별로 조회
    List<DailySettlement> findByUserIdAndSettlementDateBetween (Long userId, LocalDate startDate, LocalDate endDate);
}
