package com.homesweet.homesweetback.domain.product.product.repository.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 제품 상세 이미지 엔티티
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 20.
 */
@Entity
@Table(name = "products_detail_image")
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProductDetailImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
