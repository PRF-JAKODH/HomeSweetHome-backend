package com.homesweet.homesweetback.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    // Order와 1:1 관계. (Order가 생성되어야 Payment가 존재)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "pg_transaction_id", nullable = false, length = 255)
    private String pgTransactionId; // 토스 paymentKey

    @Column(nullable = false)
    private Long amount; // 실제 결제 승인된 금액

    @Column(nullable = false, length = 20)
    private String method; // 결제 수단 (e.g., "카드")

    @Column(name = "payment_status", nullable = false, length = 15)
    private String paymentStatus; // 결제 상태 (e.g., "DONE")

    @Column(name = "paid_at")
    private LocalDateTime paidAt; // 결제 승인 시각

    @Column(name = "pg_raw_data", columnDefinition = "JSON")
    private String pgRawData; // PG사 응답 원본 (JSON)

    @Builder
    public Payment(Order order, String pgTransactionId, Long amount, String method, String paymentStatus, LocalDateTime paidAt, String pgRawData) {
        this.order = order;
        this.pgTransactionId = pgTransactionId;
        this.amount = amount;
        this.method = method;
        this.paymentStatus = paymentStatus;
        this.paidAt = paidAt;
        this.pgRawData = pgRawData;
    }
}