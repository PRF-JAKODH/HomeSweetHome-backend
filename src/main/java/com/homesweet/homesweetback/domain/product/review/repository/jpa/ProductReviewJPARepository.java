package com.homesweet.homesweetback.domain.product.review.repository.jpa;

import com.homesweet.homesweetback.domain.product.review.repository.jpa.entity.ProductReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 제품 리뷰 JPA 레포
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
public interface ProductReviewJPARepository extends JpaRepository<ProductReviewEntity, Long> {
}
