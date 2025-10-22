package com.homesweet.homesweetback.domain.grade.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Grade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_id")
    private Integer gradeId;

    @Column(length = 10, nullable = false)
    private String grade;

    @Setter
    @Column(name = "fee_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal feeRate;
}