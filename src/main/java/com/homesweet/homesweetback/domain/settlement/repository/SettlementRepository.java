package com.homesweet.homesweetback.domain.settlement.repository;

import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    // 특정 사용자의 정산 상태별 내역 조회
    @Query("SELECT s FROM Settlement s " +
            "JOIN s.order o WHERE s.userId =:userId " +
            "AND o.orderedAt BETWEEN :startDate AND :endDate " +
            "AND s.settlementStatus =:settlementStatus " +
            "ORDER BY o.orderedAt DESC")
    List<Settlement> findByUserIdAndOrderOrderedAtBetweenAndSettlementStatus(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("settlementStatus") String settlementStatus);
    // 1. 주문건별에서 조회
    @Query("SELECT s FROM Settlement s JOIN s.order o " +
            "WHERE o.orderedAt BETWEEN :startDate AND :endDate " +
            "ORDER BY o.orderedAt")
    List<Settlement> findBySettlement(LocalDateTime startDate, LocalDateTime endDate);

    // 특정 판매자의 정산 완료율
    @Query("SELECT s FROM Settlement s JOIN s.order o " +
            "WHERE s.userId = :userId AND o.orderedAt " +
            "BETWEEN :startDate AND :endDate " +
            "ORDER BY o.orderedAt")
    List<Settlement> findByUserIdAndOrderedAtBetween(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // 사용자 조회
    List<Settlement> findByUserId(Long userId);
}