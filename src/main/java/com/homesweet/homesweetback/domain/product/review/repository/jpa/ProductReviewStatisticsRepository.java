package com.homesweet.homesweetback.domain.product.review.repository.jpa;

import com.homesweet.homesweetback.domain.product.review.repository.jpa.entity.ProductReviewStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 상품 리뷰 통계 데이터 레포
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 18.
 */
public interface ProductReviewStatisticsRepository extends JpaRepository<ProductReviewStatistics, Long> {
}
