package com.homesweet.homesweetback.domain.search.product.event;

import com.homesweet.homesweetback.common.event.DomainEvent;
import lombok.Getter;

/**
 * 상품 이벤트
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
@Getter
public class ProductEvent extends DomainEvent {
    private final Long productId;
    private final ProductEventType productEventType;

    protected ProductEvent(Long productId, ProductEventType eventType) {
        super("product." + eventType.name().toLowerCase());
        this.productId = productId;
        this.productEventType = eventType;
    }

    public static ProductEvent created(Long productId) {
        return new ProductEvent(productId, ProductEventType.CREATED);
    }

    public static ProductEvent updated(Long productId) {
        return new ProductEvent(productId, ProductEventType.UPDATED);
    }

    public static ProductEvent deleted(Long productId) {
        return new ProductEvent(productId, ProductEventType.DELETED);
    }

    public static ProductEvent statusChanged(Long productId) {
        return new ProductEvent(productId, ProductEventType.STATUS_CHANGED);
    }
}
