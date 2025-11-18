package com.homesweet.homesweetback.domain.product.review.repository.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 상품 통계 테이블
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 18.
 */
@Entity
@Table(name = "product_review_statistics")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProductReviewStatistics {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    private double averageRating;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void update(int reviewCount, double averageRating) {
        this.reviewCount = reviewCount;
        this.averageRating = averageRating;
    }
}
