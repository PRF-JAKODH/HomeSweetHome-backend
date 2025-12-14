package com.homesweet.homesweetback.domain.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 결제 엔티티
 */
@Entity
@Table(name = "payments")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "payment_key", nullable = false, length = 200)
    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false)
    private Long amount;

    @Column(length = 20)
    private String method;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // ===== 비즈니스 메서드 =====

    public void complete(String method) {
        this.status = PaymentStatus.DONE;
        this.method = method;
        this.paidAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELLED;
    }
}
