package com.homesweet.homesweetback.domain.product.product.repository.jpa;

import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductDetailImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 제품 상세 이미지 JPA 레포지토리
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
public interface ProductDetailImageJPARepository extends JpaRepository<ProductDetailImageEntity, Long> {
}
