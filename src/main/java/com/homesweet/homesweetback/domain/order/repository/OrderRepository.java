//OrderService(비즈니스 로직)가 Order(엔티티)를 DB에 저장하거나 조회해야 할 때, Service가 직접 SQL 쿼리문을 짜지 않기 위해 만듦.
package com.homesweet.homesweetback.domain.order.repository;
//package: 이 코드가 속한 폴더 경로를 지정하는 키워드

import com.homesweet.homesweetback.common.exception.OrderNotFoundException;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

//스프링 빈
public interface OrderRepository extends JpaRepository<Order, Long> {
    //  특정 사용자의 모든 주문 목록을 주문일 내림차순(최신순)으로 조회함. - 주문 정보 목록 용
    @Query("SELECT o FROM Order o " +
            "JOIN FETCH o.user u " +
            "LEFT JOIN FETCH o.orderItems oi " + // 주문 항목이 없는 경우도 있으므로 LEFT JOIN
            "LEFT JOIN FETCH oi.sku s " +
            "LEFT JOIN FETCH s.product p " +
            "WHERE o.user.id = :userId " +
            "ORDER BY o.orderedAt DESC")
    List<Order> findAllByUserWithDetails(@Param("userId") Long userId);

    // 주문 상세 정보 조회 용
    @Query("SELECT o FROM Order o " +
            "JOIN FETCH o.user u " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.sku s " +
            "LEFT JOIN FETCH s.product p " +
            "LEFT JOIN FETCH p.seller sel " + // 판매자 정보
//            "LEFT JOIN FETCH s.skuOptions so " + // SKU 옵션 정보
//            "LEFT JOIN FETCH so.optionValue ov " + // 옵션 값
            "WHERE o.id = :orderId")
    Optional<Order> findByIdWithDetails(@Param("orderId") Long orderId);

    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * 특정 상태(status)이면서, 특정 시간(cutoffTime) 이전에 생성된 모든 주문을 조회합니다.
     * (스케줄러가 PENDING 주문을 찾기 위해 사용)
     * 재고 복구를 위해 orderItems와 sku를 fetch join 할거라능.
     */
    @Query("SELECT o FROM Order o " +
            "JOIN FETCH o.orderItems oi " +
            "JOIN FETCH oi.sku s " +
            "WHERE o.orderStatus = :orderStatus AND o.orderedAt < :cutoffTime")
    List<Order> findAllByOrderStatusAndOrderedAtBefore(OrderStatus orderStatus, LocalDateTime cutoffTime);

    /**
     * ID로 주문을 조회하고, 없으면 예외를 던지는 편의 메서드
     */
    default Order getByIdWithDetailsOrThrow(Long orderId) {
        return findByIdWithDetails(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다: " + orderId));
    }

    // Number로도 했나?
    default Order getByOrderNumberOrThrow(String orderNumber) {
        return findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다: " + orderNumber));
    }

    @Query("SELECT o FROM Order o JOIN FETCH o.orderItems oi JOIN FETCH oi.sku WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);
}