package com.homesweet.homesweetback.domain.settlement.repository;

import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WeeklySettlementRepository extends JpaRepository<WeeklySettlement, Long> {

    // 특정 사용자의 연도별 조회 - 주차별 정렬
    List<WeeklySettlement> findByUserIdAndYearOrderByWeekStartDateAsc(Long userId, Short year);

    // 특정 사용자의 연도-월별 조회
    List<WeeklySettlement> findByUserIdAndYearAndMonthOrderByWeekStartDateAsc(Long userId, Short year, Byte month);

    // 특정 사용자의 주간 정산 내역
    List<WeeklySettlement> findByUserIdAndWeekStartDateBetween(
            Long userId, LocalDate weekStartDate, LocalDate weekEndDate
    );
}
