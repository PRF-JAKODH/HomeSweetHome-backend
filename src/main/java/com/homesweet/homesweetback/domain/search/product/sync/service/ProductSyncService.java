package com.homesweet.homesweetback.domain.search.product.sync.service;

import com.homesweet.homesweetback.domain.product.category.domain.ProductCategory;
import com.homesweet.homesweetback.domain.product.category.service.ProductCategoryService;
import com.homesweet.homesweetback.domain.product.product.command.domain.Product;
import com.homesweet.homesweetback.domain.product.product.command.repository.ProductRepository;
import com.homesweet.homesweetback.domain.search.product.repository.document.ProductDocument;
import com.homesweet.homesweetback.domain.search.product.sync.repository.ProductDocumentRepository;
import com.homesweet.homesweetback.domain.search.product.sync.mapping.ProductDocumentMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 부분 인덱싱 서비스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncService {

    private final ProductDocumentRepository productDocumentRepository;
    private final ProductRepository productRepository;
    private final ProductCategoryService productCategoryService;
    private final ProductDocumentMapping productDocumentMapping;

    @Transactional(readOnly = true)
    public void syncToElasticsearch(Long productId) {
        try {
            Product product = productRepository.findByProductId(productId);

            ProductCategory category = productCategoryService.getCategoryById(product.getCategoryId());

            ProductDocument document = productDocumentMapping.convertToDocument(product, category);

            productDocumentRepository.save(document);
            log.info("성공적으로 엘라스틱서치에 동기화가 되었습니다 -> productId: {}", productId);
        } catch (Exception e) {
            log.error("Failed to sync product {} to Elasticsearch", productId, e);
        }
    }

    @Transactional
    public void deleteFromElasticsearch(Long productId) {
        try {
            productDocumentRepository.deleteById(productId);
            log.info("엘라스틱서치에 상품 데이터 삭제가 성공했습니다!");
        } catch (Exception e) {
            log.error("Failed to delete product {} from Elasticsearch", productId, e);
        }
    }
}
