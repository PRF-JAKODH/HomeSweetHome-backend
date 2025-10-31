package com.homesweet.homesweetback.domain.product.cart.repository.jpa.entity;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 장바구니 엔티티
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 20.
 */
@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CartEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private SkuEntity sku;

    @Column(nullable = false)
    private Integer quantity = 1;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void updateQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @Builder
    public CartEntity(Long id, User user, SkuEntity sku, Integer quantity, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.sku = sku;
        this.quantity = quantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
