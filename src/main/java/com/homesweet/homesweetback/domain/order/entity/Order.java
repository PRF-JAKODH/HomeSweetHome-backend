package com.homesweet.homesweetback.domain.order.entity;

import com.homesweet.homesweetback.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    // (N:1) 한 명의 사용자(User)는 여러 주문(Order)을 생성 가능
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // (1:N) 한 주문은 여러 개의 SKU(상품 옵션)을 포함 */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

//    주문 상태 (결제, 취소 등)
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20)
    private OrderStatus orderStatus;

    // 배송 상태 (배송 준비, 배송 중, 배송 완료 등)
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    private DeliveryStatus deliveryStatus;

    // 총 결제 금액
    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    //주문 일시
    @CreatedDate
    @Column(name = "ordered_at", nullable = false, updatable = false)
    private LocalDateTime orderedAt;

    // 마지막 수정 시각
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 연관관계 편의 메서드 (양방향 관계 동기화)
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    @Builder
    public Order(User user, OrderStatus orderStatus, DeliveryStatus deliveryStatus, Long totalAmount) {
        this.user = user;
        this.orderStatus = orderStatus;
        this.deliveryStatus = deliveryStatus;
        this.totalAmount = totalAmount;
    }
}