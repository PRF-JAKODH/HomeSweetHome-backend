package com.homesweet.homesweetback.domain.product.review.repository.mapper;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.review.domain.ProductReview;
import com.homesweet.homesweetback.domain.product.review.repository.jpa.entity.ProductReviewEntity;
import org.springframework.stereotype.Component;

/**
 * 제품 리뷰 도메인 <-> 엔티티 매핑 클래스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
@Component
public class ProductReviewMapper {

    /**
     * Entity -> Domain 변환
     */
    public ProductReview toDomain(ProductReviewEntity entity) {
        return ProductReview.builder()
                .id(entity.getId())
                .productId(entity.getProduct().getId())
                .userId(entity.getUser().getId())
                .rating(entity.getRating())
                .comment(entity.getComment())
                .imageUrl(entity.getImageUrl())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Domain -> Entity 변환
     */
    public ProductReviewEntity toEntity(ProductReview domain) {
        return ProductReviewEntity.builder()
                .product(ProductEntity.builder().id(domain.productId()).build())
                .user(User.builder().id(domain.userId()).build())
                .rating(domain.rating())
                .comment(domain.comment())
                .imageUrl(domain.imageUrl())
                .build();
    }
}
