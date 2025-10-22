package com.homesweet.homesweetback.domain.product.product.repository.jpa;

import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 재고 JPA 레포
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
public interface SkuJPARepository extends JpaRepository<SkuEntity, Long> {
}
