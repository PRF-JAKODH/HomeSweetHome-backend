package com.homesweet.homesweetback.domain.product.category.repository;

import com.homesweet.homesweetback.domain.product.category.domain.ProductCategory;

import java.util.List;
import java.util.Optional;

/**
 * 제품 카테고리 레포 인터페이스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
public interface ProductCategoryRepository {

    ProductCategory save(ProductCategory productCategory);

    Optional<ProductCategory> findById(Long id);

    Optional<ProductCategory> findByName(String name);

    List<ProductCategory> findByParentId(Long parentId);

    List<ProductCategory> findByDepth(Integer depth);

}
