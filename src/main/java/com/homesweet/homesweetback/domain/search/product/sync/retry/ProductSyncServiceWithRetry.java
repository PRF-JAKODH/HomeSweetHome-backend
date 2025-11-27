package com.homesweet.homesweetback.domain.search.product.sync.retry;

import com.homesweet.homesweetback.domain.search.product.sync.service.ProductSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * 상품 엘라스틱 부분 인덱싱 실패 처리
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncServiceWithRetry {

    private final ProductSyncService productSyncService;

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            retryFor = {Exception.class}
    )
    public void syncWithRetry(Long productId) {
        log.info("상품 엘라스틱 동기화 재시도 처리");
        productSyncService.syncToElasticsearch(productId);
    }
}
