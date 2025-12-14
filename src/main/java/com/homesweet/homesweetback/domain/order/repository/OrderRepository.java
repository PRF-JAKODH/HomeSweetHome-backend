package com.homesweet.homesweetback.domain.order.repository;

import com.homesweet.homesweetback.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);
}
