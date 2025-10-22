package com.homesweet.homesweetback.domain.product.product.repository;

import com.homesweet.homesweetback.domain.product.product.domain.Product;

/**
 * 제품 레포 인터페이스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
public interface ProductRepository {

    Product save(Product product);

    boolean existsBySellerIdAndName(Long sellerId, String name);
}
