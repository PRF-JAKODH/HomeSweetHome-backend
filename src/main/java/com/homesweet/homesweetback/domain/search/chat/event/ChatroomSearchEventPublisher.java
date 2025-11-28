package com.homesweet.homesweetback.domain.search.chat.event;

import com.homesweet.homesweetback.common.event.DomainEvent;
import com.homesweet.homesweetback.common.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 채팅방 이벤트 Publisher
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 28.
 */
@Component
@RequiredArgsConstructor
@Qualifier("chatroomEventPublisher")
public class ChatroomSearchEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(DomainEvent event) {
        eventPublisher.publishEvent(event);
    }
}
