package com.homesweet.homesweetback.domain.product.product.repository;

import com.homesweet.homesweetback.domain.product.product.domain.Sku;

import java.util.Optional;

/**
 * 제품 옵션 레포 인터페이스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
public interface SkuRepository {

    Optional<Sku> findById(Long id);

    boolean existsById(Long id);
}
