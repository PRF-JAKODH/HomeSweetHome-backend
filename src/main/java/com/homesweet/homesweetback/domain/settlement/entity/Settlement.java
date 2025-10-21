package com.homesweet.homesweetback.domain.settlement.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long settlementId;

    private Long userId;

    @Column(name = "settlement_status", length = 10)
    private String settlementStatus;

    @Column(name = "sales_amount", precision = 15, scale = 2)
    private BigDecimal salesAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal fee;

    @Column(precision = 15, scale = 2)
    private BigDecimal vat;

    @Column(name = "refund_amount", precision = 15, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "settlement_amount", precision = 15, scale = 2)
    private BigDecimal settlementAmount;

    private LocalDateTime settlementDate;

    @Column(nullable = false)
    private Boolean orderCanceled;

    // 주문 정보 fk
//    @ManyToOne
//    @JoinColumn(name = "order_id")
//    private Order order;
}