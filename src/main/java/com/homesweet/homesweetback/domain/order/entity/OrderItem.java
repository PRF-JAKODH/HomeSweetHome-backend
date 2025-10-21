package com.homesweet.homesweetback.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // (연관관계 편의 메서드용)

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    // (ERD 기반) 상품 ID - Product 엔티티와 연관관계 매핑 필요
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "product_id")
    // private Product product;
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity; // 주문 수량

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice; // 주문 당시 개당 가격 (스냅샷)

    // OrderItem은 Order를 알아야 함 (N:1)
    @Setter // (연관관계 편의 메서드에서만 사용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // (선택) 배송 상태. (우리는 Order의 상태를 따르기로 했으므로 여기선 생략 가능)
    // @Column(name = "order_item_status", length = 15)
    // private String orderItemStatus;

    @Builder
    public OrderItem(Long productId, int quantity, Long unitPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
}