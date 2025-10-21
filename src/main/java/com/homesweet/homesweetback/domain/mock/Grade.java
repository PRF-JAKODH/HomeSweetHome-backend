package com.homesweet.homesweetback.domain.mock;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 판매자 등급 엔티티
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 20.
 */

@Entity
@Table(name = "grade")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_id")
    private Integer gradeId;

    @Column(name = "grade", length = 10, nullable = false)
    private String grade;

    @Column(name = "fee_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal feeRate;

}
