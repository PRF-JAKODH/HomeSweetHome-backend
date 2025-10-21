package com.homesweet.homesweetback.domain.settlement.repository;

import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    // 특정 사용자의 정산 내역을 주문 일시(거래 일시 Order.orderDate)로 내역 조회
    @Query(value = "SELECT s FROM Settlement s " +
            "JOIN s.order o WHERE s.userId = :userId AND o.orderDate BETWEEN :startDate AND :endDate " +
            "ORDER BY o.orderDate DESC")
    List<Settlement> findByOrderDate(
            @Param("userId") Long userId,
            @Param("startDate")LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // 특정 사용자의 전체 정산 내역을 조회
    List<Settlement> findByUserId(Long userId);

    // 정산 상태별 내역 조회
    List<Settlement> findBySettlementStatus(String settlementStatus);
}
