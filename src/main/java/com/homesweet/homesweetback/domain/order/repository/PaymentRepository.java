package com.homesweet.homesweetback.domain.order.repository;

import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrder(Order order);

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByPaymentKey(String paymentKey);

    boolean existsByPaymentKey(String paymentKey);
}
