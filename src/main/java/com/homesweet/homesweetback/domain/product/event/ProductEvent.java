package com.homesweet.homesweetback.domain.product.event;

import lombok.Getter;

/**
 * 상품 이벤트
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
@Getter
public class ProductEvent {
    private final Long productId;
    private final EventType eventType;
    private final Long timestamp;

    private ProductEvent(Long productId, EventType eventType) {
        this.productId = productId;
        this.eventType = eventType;
        this.timestamp = System.currentTimeMillis();
    }

    public static ProductEvent created(Long productId) {
        return new ProductEvent(productId, EventType.CREATED);
    }

    public static ProductEvent updated(Long productId) {
        return new ProductEvent(productId, EventType.UPDATED);
    }

    public static ProductEvent deleted(Long productId) {
        return new ProductEvent(productId, EventType.DELETED);
    }

    public static ProductEvent statusChanged(Long productId) {
        return new ProductEvent(productId, EventType.STATUS_CHANGED);
    }
}
