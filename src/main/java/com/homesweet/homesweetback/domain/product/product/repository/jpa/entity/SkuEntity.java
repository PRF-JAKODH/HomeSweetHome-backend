package com.homesweet.homesweetback.domain.product.product.repository.jpa.entity;

import com.homesweet.homesweetback.domain.product.cart.repository.jpa.entity.CartEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
@NoArgsConstructor
public class SkuEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sku_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "price_adjustment")
    private Integer priceAdjustment = 0;

    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;

}
