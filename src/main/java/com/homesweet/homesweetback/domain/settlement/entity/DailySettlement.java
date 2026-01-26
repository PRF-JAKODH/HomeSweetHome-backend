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
@Table(name = "daily_settlements")
public class DailySettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_id")
    private Long dailyId;

    private Long userId;

    @Column(name = "total_sales", precision = 15, scale = 2)
    private BigDecimal totalSales;

    @Column(name = "total_fee", precision = 15, scale = 2)
    private BigDecimal totalFee;

    @Column(name = "total_vat", precision = 15, scale = 2)
    private BigDecimal totalVat;

    @Column(name = "total_refund", precision = 15, scale = 2)
    private BigDecimal totalRefund;

    @Column(name = "total_settlement", precision = 15, scale = 2)
    private BigDecimal totalSettlement;

    private LocalDateTime settlementDate;
}