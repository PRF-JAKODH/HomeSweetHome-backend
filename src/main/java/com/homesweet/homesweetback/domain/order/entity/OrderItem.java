package com.homesweet.homesweetback.domain.order.entity;

import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;


    // (N:1) 한 주문은 여러 주문 상품을 가질 수 있다/
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // (N:1) 하나의 주문 상품은 하나의 SKU(옵션)를 가리킨다
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private SkuEntity sku;

    // 주문 수량
    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Column(name = "price", nullable = false)
    private Long price;

    @Builder
    public OrderItem(SkuEntity sku, Long quantity, Long price) {
        this.sku = sku;
        this.quantity = quantity;
        this.price = price;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}