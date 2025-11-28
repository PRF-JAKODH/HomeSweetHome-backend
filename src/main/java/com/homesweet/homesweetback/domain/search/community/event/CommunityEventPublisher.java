package com.homesweet.homesweetback.domain.search.community.event;

import com.homesweet.homesweetback.common.event.DomainEvent;
import com.homesweet.homesweetback.common.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 게시글 이벤트 Publisher
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
@Component
@RequiredArgsConstructor
@Qualifier("communityEventPublisher")
public class CommunityEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(DomainEvent event) {
        eventPublisher.publishEvent(event);
    }
}
