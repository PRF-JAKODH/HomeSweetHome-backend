package com.homesweet.homesweetback.domain.settlement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "monthly_settlements")
public class MonthlySettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_id")
    private Long monthlyId;

    private Long userId;

    @Column(nullable = false)
    private Short year;

    @Column(nullable = false)
    private Byte month;

    @Column(name = "total_sales", precision = 15, scale = 2)
    private BigDecimal totalSales;

    @Column(name = "total_fee", precision = 15, scale = 2)
    private BigDecimal totalFee;

    @Column(name = "total_refund", precision = 15, scale = 2)
    private BigDecimal totalRefund;

    @Column(name = "total_settlement", precision = 15, scale = 2)
    private BigDecimal totalSettlement;
}