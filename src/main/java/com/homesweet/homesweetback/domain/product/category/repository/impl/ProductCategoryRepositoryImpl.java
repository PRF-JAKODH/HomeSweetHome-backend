package com.homesweet.homesweetback.domain.product.category.repository.impl;

import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.ProductCategoryJPARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 제품 카테고리 레포 구현 코드 -> JPA 사용
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@Repository
@RequiredArgsConstructor
public class ProductCategoryRepositoryImpl implements ProductCategoryRepository {

    private final ProductCategoryJPARepository productCategoryJPARepository;

}
