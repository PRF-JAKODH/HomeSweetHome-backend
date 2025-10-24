package com.homesweet.homesweetback.domain.product.product.repository.jpa;

import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 제품 제고 JPA 레포
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
public interface SkuJPARepository extends JpaRepository<SkuEntity, Long> {

    Optional<SkuEntity> findById(Long id);
}
