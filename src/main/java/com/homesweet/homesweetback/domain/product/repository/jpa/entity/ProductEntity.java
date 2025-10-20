package com.homesweet.homesweetback.domain.product.repository.jpa.entity;

import com.homesweet.homesweetback.domain.product.dto.type.ProductStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 상품 엔티티
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 20.
 */

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User seller;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column(nullable = false, length = 20)
    private String brand;

    @Column(name = "base_price", nullable = false)
    private Integer basePrice = 0;

    @Column(name = "discount_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountRate = BigDecimal.ZERO;

    @Column(length = 255)
    private String description;

    @Column(name = "shipping_price", nullable = false)
    private Integer shippingPrice = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ProductStatus status;

    @Column(name = "option_type", nullable = false, length = 12)
    @Enumerated(EnumType.STRING)
    private ProductOptionType optionType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductDetailImage> detailImages = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sku> skus = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductReview> reviews = new ArrayList<>();
}
