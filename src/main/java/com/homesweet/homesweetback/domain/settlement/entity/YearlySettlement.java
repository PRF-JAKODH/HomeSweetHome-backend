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
@Table(name = "yearly_settlements")
public class YearlySettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "yearly_id")
    private Long yearlyId;

    private Long userId; // 어떤 사용자의 정산인지만 확인하는 것이기 때문에 객체를 참조할 필요 X

    @Column(nullable = false)
    private Short year;

    @Column(name = "total_sales", precision = 15, scale = 2)
    private BigDecimal totalSales;

    @Column(name = "total_fee", precision = 15, scale = 2)
    private BigDecimal totalFee;

    @Column(name = "total_refund", precision = 15, scale = 2)
    private BigDecimal totalRefund;

    @Column(name = "total_settlement", precision = 15, scale = 2)
    private BigDecimal totalSettlement;
}
