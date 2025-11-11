package com.homesweet.homesweetback.domain.settlement.repository;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.settlement.dto.response.SettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;

import org.aspectj.weaver.ast.Or;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    // 판매자 조회
    @Query("""
    SELECT p.seller FROM Order o
        JOIN o.orderItems oi
        JOIN oi.sku s
        JOIN s.product p
        WHERE o.id =:orderId
    """)
    User findBySellerId(@Param("orderId") Long orderId);

    // 주문건별에서 전체 목록 조회
    @Query("""
        SELECT o.orderedAt, o.orderNumber,
            CONCAT(MIN(p.name), CASE WHEN COUNT(oi) > 1 THEN CONCAT (' 외 ', (COUNT(oi) - 1), '개') ELSE '' END)
           , s.salesAmount,s.fee,s.vat, s.refundAmount, s.settlementAmount,s.settlementDate, s.settlementStatus
        FROM Settlement s
        JOIN s.order o
            JOIN o.orderItems oi JOIN oi.sku sku JOIN sku.product p
        WHERE s.userId =:userId
          AND o.orderedAt >= :startDate
          AND o.orderedAt < :endDate
          AND (:settlementStatus IS NULL OR :settlementStatus = '' OR s.settlementStatus = :settlementStatus)
        GROUP BY o.orderedAt, o.orderNumber, s.salesAmount, s.fee, s.vat, s.refundAmount, s.settlementAmount, s.settlementDate, s.settlementStatus
        ORDER BY o.orderedAt DESC
    """)
    Page<SettlementResponse> findBySettlement(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("settlementStatus") String settlementStatus, Pageable pageable);

    // 정산일 기준 집계에서 사용
    @Query("""
        SELECT s
        FROM Settlement s
        WHERE s.userId = :userId
          AND s.settlementDate >= :startDate
          AND s.settlementDate <  :endDate
        ORDER BY s.settlementDate DESC, s.settlementId DESC
    """)
    List<Settlement> findBySettlementDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // 집계시 정산 상태 변경
    @Modifying
    @Transactional
    @Query("""
      UPDATE Settlement s
      SET s.settlementStatus = 'COMPLETED'
      WHERE s.userId = :userId
        AND s.settlementDate >= :startDate
        AND s.settlementDate <  :endDate
        AND s.settlementStatus = 'PENDING'
    """)
    int markCompletedInRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // 정산 완료율 계산
    @Query("""
    SELECT COUNT(s) FROM Settlement s
    JOIN s.order o
    WHERE s.userId =:userId AND s.settlementStatus='COMPLETED'
    AND o.orderedAt>= :startDate And o.orderedAt < :endDate
    """)
    long countCompletedSettlements(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    // 총 주문건수 계산
    @Query("""
        SELECT COUNT(s)
        FROM Settlement s JOIN s.order o
        WHERE s.userId = :userId
        AND o.orderedAt >= :startDate AND o.orderedAt < :endDate
    """)
    long countAllByOrderedAt(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    // 주문 취소시 환불금액 생성 및 정산 금액 변경
    @Modifying
    @Query("""
    UPDATE Settlement s
    SET s.refundAmount = s.refundAmount + :refundAmount,
        s.settlementAmount = (s.salesAmount + s.vat) - s.fee - (s.refundAmount + :refundAmount),
        s.settlementStatus = 'CANCELED'
    WHERE s.order.id =:orderId
    """)
    int applyRefundAmount(@Param("orderId") Long orderId, @Param("refundAmount") BigDecimal refundAmount);

    Optional<Settlement>findByOrderId(@Param("orderId")Long orderId);

    // 신규 정산건 찾기
    @Query("""
    SELECT o FROM Order o
    LEFT JOIN Settlement s ON o.id = s.order.id
    WHERE o.orderStatus =:orderStatus
    AND o.orderedAt <= :cutOffTime
    AND s.settlementId IS NULL
    """)
    List<Order> findUnSettlementOrders(@Param("orderStatus") OrderStatus orderStatus, @Param("cutOffTime") LocalDateTime cutoffTime);

    // 정산 취소건 찾기
    @Query("""
    SELECT o FROM Order o
    LEFT JOIN Settlement s ON o.id = s.order.id
    WHERE o.deliveryStatus = :deliveryStatus
    AND o.orderedAt <= :cutOffTime
    AND s.settlementId IS NOT NULL
    """)
    List<Order> findCancelSettlement(@Param("deliveryStatus")DeliveryStatus deliveryStatus, @Param("cutOffTime") LocalDateTime cutoffTime);

}