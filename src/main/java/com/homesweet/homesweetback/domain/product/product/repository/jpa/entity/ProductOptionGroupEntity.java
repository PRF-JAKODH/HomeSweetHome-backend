package com.homesweet.homesweetback.domain.product.product.repository.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 상품 옵션 그룹 엔티티
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
@Entity
@Table(name = "product_option_group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ProductOptionGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_group_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String groupName;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOptionValueEntity> values = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public ProductOptionGroupEntity(String groupName) {
        this.groupName = groupName;
    }

    public void addOptionValue(ProductOptionValueEntity value) {
        values.add(value);
        value.setGroup(this);
    }
}