package com.homesweet.homesweetback.domain.settlement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "weekly_settlements")
public class WeeklySettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weekly_id")
    private Long weeklyId;

    private Long userId;

    @Column(nullable = false)
    private Short year;

    @Column(nullable = false)
    private Byte month;

    private LocalDate weekStartDate;

    private LocalDate weekEndDate;

    @Column(name = "daily_sales", precision = 15, scale = 2)
    private BigDecimal dailySales;

    @Column(name = "weekly_sales", precision = 15, scale = 2)
    private BigDecimal weeklySales;

    @Column(name = "total_sales", precision = 15, scale = 2)
    private BigDecimal totalSales;

    @Column(name = "total_fee", precision = 15, scale = 2)
    private BigDecimal totalFee;

    @Column(name = "total_refund", precision = 15, scale = 2)
    private BigDecimal totalRefund;

    @Column(name = "total_settlement", precision = 15, scale = 2)
    private BigDecimal totalSettlement;
}