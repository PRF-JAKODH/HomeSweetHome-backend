package com.homesweet.homesweetback.domain.product.product.repository.jpa.entity;

import com.homesweet.homesweetback.domain.product.cart.repository.jpa.entity.CartEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * 제품 재고 엔티티
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 20.
 */
@Entity
@Table(name = "sku")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Builder
@AllArgsConstructor
public class SkuEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sku_id")
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "price_adjustment", nullable = false)
    private Integer priceAdjustment = 0;

    @Column(name = "stock_quantity", nullable = false)
    private Long stockQuantity = 0L;

    @BatchSize(size = 100)
    @Builder.Default
    @OneToMany(mappedBy = "sku", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSkuOptionEntity> skuOptions = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public SkuEntity(Integer priceAdjustment, Long stockQuantity) {
        this.priceAdjustment = priceAdjustment;
        this.stockQuantity = stockQuantity;
    }

    public void addSkuOption(ProductSkuOptionEntity skuOption) {
        this.skuOptions.add(skuOption);
        skuOption.setSku(this);
    }

    public void decreaseStock(Long quantity) {
        if (this.stockQuantity < quantity) {
            throw new RuntimeException("재고 부족"); // (추후 커스텀 예외)
        }
        this.stockQuantity -= quantity;
    }

    public void updateStock(Long newStockQuantity, Integer newPriceAdjustment) {
        this.stockQuantity = newStockQuantity;
        if (newPriceAdjustment != null) {
            this.priceAdjustment = newPriceAdjustment;
        }
    }
}
