//OrderService(비즈니스 로직)가 Order(엔티티)를 DB에 저장하거나 조회해야 할 때, Service가 직접 SQL 쿼리문을 짜지 않기 위해 만듦.
package com.homesweet.homesweetback.domain.order.repository;
//package: 이 코드가 속한 폴더 경로를 지정하는 키워드

import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
            "WHERE o.user = :user " +
            "ORDER BY o.orderedAt DESC")
    List<Order> findAllByUserWithDetails(@Param("user") User user);

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
}