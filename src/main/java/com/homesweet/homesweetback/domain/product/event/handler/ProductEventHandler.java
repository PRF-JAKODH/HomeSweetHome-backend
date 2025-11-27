package com.homesweet.homesweetback.domain.product.event.handler;

import com.homesweet.homesweetback.domain.product.event.ProductEvent;
import com.homesweet.homesweetback.domain.search.product.sync.service.ProductSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 상품 이벤트 핸들러
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventHandler {

    private final ProductSyncService productSyncService;

    @Async("productEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductEvent(ProductEvent event) {
        log.info("[상품 이벤트] 실행 : type={}, productId={}", event.getEventType(), event.getProductId());

        try {
            switch (event.getEventType()) {
                case CREATED, UPDATED, STATUS_CHANGED -> productSyncService.syncToElasticsearch(event.getProductId());
                case DELETED -> productSyncService.deleteFromElasticsearch(event.getProductId());
            }
        } catch (Exception e) {
            log.error("Failed to handle product event: {}", event, e);
        }
    }
}
