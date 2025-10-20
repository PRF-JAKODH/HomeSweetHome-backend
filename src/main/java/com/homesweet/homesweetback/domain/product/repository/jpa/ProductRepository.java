package com.homesweet.homesweetback.domain.product.repository.jpa;

import com.homesweet.homesweetback.domain.product.repository.jpa.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Product Repository
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 20.
 */
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
}
