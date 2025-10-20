package com.homesweet.homesweetback.domain.product.product.repository.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 제품 옵션 엔티티
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 20.
 */
@Entity
@Table(name = "product_option")
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProductOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "option_name", nullable = false, length = 12)
    private String optionName;

    @Column(nullable = false, length = 12)
    private String value;

    @OneToMany(mappedBy = "option")
    private List<ProductSkuOptionEntity> skuOptions = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;
}
