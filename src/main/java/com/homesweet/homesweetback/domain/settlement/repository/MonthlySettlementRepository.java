package com.homesweet.homesweetback.domain.settlement.repository;

import com.homesweet.homesweetback.domain.settlement.entity.MonthlySettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonthlySettlementRepository extends JpaRepository<MonthlySettlement, Long> {
    // 특정 사용자와 연도 조회 - 정렬
    List<MonthlySettlement> findByUserIdAndYearOrderByMonthAsc(Long userId, Short year);

    // 연도와 월을 이용해 조회
    List<MonthlySettlement> findByUserIdAndYearAndMonth(Long userId, Short year, Byte month);
}
