package com.homesweet.homesweetback.domain.product.product.query.repository.document;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

/**
 * 상품 Elastic 레포
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 24.
 */
public interface ProductDocumentRepository extends ElasticsearchRepository<ProductDocument, Long> {

}
