package com.homesweet.homesweetback.common.event;

/**
 * 이벤트 처리
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
public interface EventConsumer {

    // 이벤트 처리
    void handle(DomainEvent event);
}
