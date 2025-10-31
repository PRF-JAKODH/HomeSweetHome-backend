package com.homesweet.homesweetback.domain.settlement.entity;

import com.homesweet.homesweetback.domain.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long settlementId;

    private Long userId;

    @Setter
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

    @Setter
    @Column(name = "settlement_amount", precision = 15, scale = 2)
    private BigDecimal settlementAmount;

    private LocalDateTime settlementDate;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}