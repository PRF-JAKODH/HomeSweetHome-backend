package com.homesweet.homesweetback.domain.product.product.repository.jpa;

import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.querydsl.CustomProductRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 제품 JPA 레포
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
public interface ProductJPARepository extends JpaRepository<ProductEntity, Long>, CustomProductRepository {

    boolean existsBySellerIdAndName(Long sellerId, String name);
}
