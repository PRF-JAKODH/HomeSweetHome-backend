package com.homesweet.homesweetback.domain.search.product.sync.repository;

import com.homesweet.homesweetback.domain.search.product.repository.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 상품 Elastic 레포
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 24.
 */
public interface ProductDocumentRepository extends ElasticsearchRepository<ProductDocument, Long> {

}
