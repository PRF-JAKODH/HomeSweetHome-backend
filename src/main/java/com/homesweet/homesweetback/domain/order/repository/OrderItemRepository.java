package com.homesweet.homesweetback.domain.order.repository;

import com.homesweet.homesweetback.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // (내용 없음. 기본 CRUD만 있으면 됩니다.)
}