package com.homesweet.homesweetback.domain.order.entity;

import jakarta.persistence;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "merchant_uid", nullable = false, unique = true, length = 100)
    private String merchantUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 15)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "ordered_at", nullable = false)
    private LocalDateTime orderedAt;

    @Column(name = "used_point")
    private Long usedPoint;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "recipient_name", nullable = false, length = 15)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 15)
    private String recipientPhone;

    @Column(name = "shipping_address", nullable = false, length = 100)
    private String shippingAddress;

    @Column(name = "shipping_request", nullable = false, length = 255)
    private String shippingRequest;

    // ===== 양방향 연관관계 =====
    // Order가 OrderItem을 관리 (CascadeType.ALL, orphanRemoval = true)
    // Order가 저장/삭제될 때 OrderItem도 함께 저장/삭제됩니다.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    // ===== 생성자 (Builder 패턴) =====
    @Builder
    public Order(String merchantUid, Long totalAmount, Long usedPoint, String shippingAddress, Long userId, OrderStatus status) {
        this.merchantUid = merchantUid;
        this.totalAmount = totalAmount;
        this.usedPoint = usedPoint;
        this.shippingAddress = shippingAddress;
        this.userId = userId;
        this.status = status;
        this.orderedAt = LocalDateTime.now();
    }

    // ===== 연관관계 편의 메서드 =====
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    // ===== 비즈니스 로직 (상태 변경) =====
    public void changeStatus(OrderStatus status) {
        this.status = status;
    }
}
