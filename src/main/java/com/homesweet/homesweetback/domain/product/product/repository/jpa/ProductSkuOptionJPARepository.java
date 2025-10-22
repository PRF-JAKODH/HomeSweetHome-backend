package com.homesweet.homesweetback.domain.product.product.repository.jpa;

import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductSkuOptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 제품 SKU 옵션 JPA 레포지토리
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
public interface ProductSkuOptionJPARepository extends JpaRepository<ProductSkuOptionEntity, Long> {

}
