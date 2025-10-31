package com.homesweet.homesweetback.domain.product.product.repository.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 재고, 옵션 중간 테이블
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 20.
 */

@Entity
@Table(name = "product_sku_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ProductSkuOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sku_option_id")
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private SkuEntity sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_value_id", nullable = false)
    private ProductOptionValueEntity optionValue;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public ProductSkuOptionEntity(Long id, SkuEntity sku, ProductOptionValueEntity optionValue) {
        this.sku = sku;
        this.optionValue = optionValue;
    }
}