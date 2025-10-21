package com.homesweet.homesweetback.domain.product.product.repository.impl;

import com.homesweet.homesweetback.domain.product.product.repository.ProductRepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.ProductJPARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 제품 레포 구현 코드
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJPARepository jpaRepository;

}
