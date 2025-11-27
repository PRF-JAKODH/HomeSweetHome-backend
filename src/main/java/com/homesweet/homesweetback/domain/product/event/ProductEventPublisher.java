package com.homesweet.homesweetback.domain.product.event;

import com.homesweet.homesweetback.common.event.DomainEvent;
import com.homesweet.homesweetback.common.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 상품 이벤트 생성
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
@Component
@RequiredArgsConstructor
public class ProductEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(DomainEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publish(String topic, DomainEvent event) {
        publish(event);
    }
}
