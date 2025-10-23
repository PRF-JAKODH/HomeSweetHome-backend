package com.homesweet.homesweetback.domain.settlement.repository;

import com.homesweet.homesweetback.domain.settlement.entity.YearlySettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface YearlySettlementRepository extends JpaRepository<YearlySettlement, Long> {
    // 특정 사용자와 연도 조회
    Optional<YearlySettlement> findByUserIdAndYear(Long userId, Short year);
}