package com.homesweet.homesweetback.common.event;

/**
 * 이벤트 발행
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
public interface EventPublisher {

    // 이벤트 발행
    void publish(DomainEvent event);

    // 이벤트 발행 (토픽, 라우팅키 지정)
    void publish(String topic, DomainEvent event);
}
