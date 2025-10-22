package com.homesweet.homesweetback.domain.product.product.repository.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 상품 옵션 값 엔티티
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
@Entity
@Table(name = "product_option_value")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ProductOptionValueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_value_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String value;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_group_id", nullable = false)
    private ProductOptionGroupEntity group;

    @OneToMany(mappedBy = "optionValue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSkuOptionEntity> skuLinks = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public ProductOptionValueEntity(String value) {
        this.value = value;
    }
}