package com.homesweet.homesweetback.domain.product.product.query.repository.impl;

import com.homesweet.homesweetback.domain.product.product.query.repository.ProductSearchRepository;
import com.homesweet.homesweetback.domain.product.product.query.repository.document.ProductDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 상품 검색 레포 구현체
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 24.
 */
@Repository
@RequiredArgsConstructor
public class ProductSearchRepositoryImpl implements ProductSearchRepository {

    private final ProductDocumentRepository productRepository;


}
